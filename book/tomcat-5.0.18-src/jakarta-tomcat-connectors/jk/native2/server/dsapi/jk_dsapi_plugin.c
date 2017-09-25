/* ========================================================================= *
 *                                                                           *
 *                 The Apache Software License,  Version 1.1                 *
 *                                                                           *
 *          Copyright (c) 1999-2001 The Apache Software Foundation.          *
 *                           All rights reserved.                            *
 *                                                                           *
 * ========================================================================= *
 *                                                                           *
 * Redistribution and use in source and binary forms,  with or without modi- *
 * fication, are permitted provided that the following conditions are met:   *
 *                                                                           *
 * 1. Redistributions of source code  must retain the above copyright notice *
 *    notice, this list of conditions and the following disclaimer.          *
 *                                                                           *
 * 2. Redistributions  in binary  form  must  reproduce the  above copyright *
 *    notice,  this list of conditions  and the following  disclaimer in the *
 *    documentation and/or other materials provided with the distribution.   *
 *                                                                           *
 * 3. The end-user documentation  included with the redistribution,  if any, *
 *    must include the following acknowlegement:                             *
 *                                                                           *
 *       "This product includes  software developed  by the Apache  Software *
 *        Foundation <http://www.apache.org/>."                              *
 *                                                                           *
 *    Alternately, this acknowlegement may appear in the software itself, if *
 *    and wherever such third-party acknowlegements normally appear.         *
 *                                                                           *
 * 4. The names  "The  Jakarta  Project",  "Jk",  and  "Apache  Software     *
 *    Foundation"  must not be used  to endorse or promote  products derived *
 *    from this  software without  prior  written  permission.  For  written *
 *    permission, please contact <apache@apache.org>.                        *
 *                                                                           *
 * 5. Products derived from this software may not be called "Apache" nor may *
 *    "Apache" appear in their names without prior written permission of the *
 *    Apache Software Foundation.                                            *
 *                                                                           *
 * THIS SOFTWARE IS PROVIDED "AS IS" AND ANY EXPRESSED OR IMPLIED WARRANTIES *
 * INCLUDING, BUT NOT LIMITED TO,  THE IMPLIED WARRANTIES OF MERCHANTABILITY *
 * AND FITNESS FOR  A PARTICULAR PURPOSE  ARE DISCLAIMED.  IN NO EVENT SHALL *
 * THE APACHE  SOFTWARE  FOUNDATION OR  ITS CONTRIBUTORS  BE LIABLE  FOR ANY *
 * DIRECT,  INDIRECT,   INCIDENTAL,  SPECIAL,  EXEMPLARY,  OR  CONSEQUENTIAL *
 * DAMAGES (INCLUDING,  BUT NOT LIMITED TO,  PROCUREMENT OF SUBSTITUTE GOODS *
 * OR SERVICES;  LOSS OF USE,  DATA,  OR PROFITS;  OR BUSINESS INTERRUPTION) *
 * HOWEVER CAUSED AND  ON ANY  THEORY  OF  LIABILITY,  WHETHER IN  CONTRACT, *
 * STRICT LIABILITY, OR TORT  (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN *
 * ANY  WAY  OUT OF  THE  USE OF  THIS  SOFTWARE,  EVEN  IF  ADVISED  OF THE *
 * POSSIBILITY OF SUCH DAMAGE.                                               *
 *                                                                           *
 * ========================================================================= *
 *                                                                           *
 * This software  consists of voluntary  contributions made  by many indivi- *
 * duals on behalf of the  Apache Software Foundation.  For more information *
 * on the Apache Software Foundation, please see <http://www.apache.org/>.   *
 *                                                                           *
 * ========================================================================= */

/***************************************************************************
 * Description: DSAPI plugin for Lotus Domino                              *
 * Author:      Andy Armstrong <andy@tagish.com>                           *
 * Version:     $Revision: 1.2 $                                           *
 ***************************************************************************/

/* Based on the IIS redirector by Gal Shachor <shachor@il.ibm.com> */

/* Standard headers */
#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <ctype.h>

/* If we're building under Windows get windows.h. This must be included
 * before any APR includes because APR itself does a #include <windows.h>
 * after turning off some features that we need.
 */
#ifdef WIN32
#include <windows.h>
#endif

#include "config.h"

/* JK stuff */
#include "jk_global.h"
#include "jk_requtil.h"
#include "jk_map.h"
#include "jk_pool.h"
#include "jk_logger.h"
#include "jk_env.h"
#include "jk_service.h"
#include "jk_worker.h"
#include "apr_general.h"

#ifndef NO_CAPI
/* Domino stuff */
#include <global.h>
#include <addin.h>
#else
#include <stdarg.h>
#define NOERROR 0
#endif

/* Domino DSAPI filter definitions */
#include "dsapifilter.h"

#ifdef TESTING
#define LOGGER				"logger.printf"
int JK_METHOD jk2_logger_printf_factory(jk_env_t *env, jk_pool_t *pool, jk_bean_t *result, const char *type, const char *name);
#else
#define LOGGER				"logger.win32"
#endif

static char  libFileName[MAX_PATH];
static char  iniFileName[MAX_PATH];
static int   iniFileUsed		= JK_FALSE;
static int   isInited			= JK_FALSE;

static const char *tomcatStart	= NULL;
static const char *tomcatStop	= NULL;
static const char *workersFile	= NULL;
static const char *serverRoot	= NULL;
static int tomcatTimeout		= TOMCAT_STARTSTOP_TO;

static const char *crlf			= "\r\n";

static jk_workerEnv_t *workerEnv;
static apr_pool_t *jk_globalPool;

/* Per request private data */
typedef struct private_ws {
	/* These get passed in by Domino and are used to access various
	 * Domino methods and data.
	 */
	FilterContext		*context;
	FilterParsedRequest	*reqData;

	/* True iff the response headers have been sent
	 */
	int					responseStarted;

	/* Current pointer into and remaining size
	 * of request body data
	 */
	char				*reqBuffer;
	unsigned int		reqSize;

} private_ws_t;

/* Case insentive memcmp() clone
 */
#ifdef HAVE_MEMICMP
#define noCaseMemCmp(ci, cj, l) _memicmp((void *) (ci), (void *) (cj), (l))
#else
static int noCaseMemCmp(const char *ci, const char *cj, int len) {
	if (0 == memcmp(ci, cj, len)) {
		return 0;
	}

	while (len > 0) {
		int cmp = tolower(*ci) - tolower(*cj);
		if (cmp != 0) {
			return cmp;
		}
		ci++;
		cj++;
		len--;
	}
	return 0;
}
#endif

/* Case insentive strcmp() clone
 */
#ifdef HAVE_STRICMP
#define noCaseStrCmp(si, sj) _stricmp((void *) (si), (void *) (sj))
#else
static int noCaseStrCmp(const char *si, const char *sj) {
	if (0 == strcmp(si, sj)) {
		return 0;
	}

	while (*si && tolower(*si) == tolower(*sj)) {
		si++;
		sj++;
	}

	return tolower(*si) - tolower(*sj);
}
#endif

/* Case insensitive substring search.
 * str		string to search
 * slen		length of string to search
 * ptn		pattern to search for
 * plen		length of pattern
 * returns	1 if there's a match otherwise 0
 */
static int scanPath(const char *str, int slen, const char *ptn, int plen) {
	const char *sp = str;

	while (slen >= plen) {
		/* We're looking for a match for the specified string bounded by
		 * the start of the string, \ or / at the left and the end of the
		 * string, \ or / at the right. We look for \ as well as / on the
		 * suspicion that a Windows hosted server might accept URIs
		 * containing \.
		 */
		if (noCaseMemCmp(sp, ptn, plen) == 0 &&
			(sp == str || sp[-1] == '\\' || sp[-1] == '/') &&
			(slen == plen || sp[plen] == '\\' || sp[plen] == '/')) {
			return 1;
		}

		slen--;
		sp++;
	}
	return 0;
}

#ifdef NO_CAPI
/* Alternative to the Domino function */
static void AddInLogMessageText(char *msg, unsigned short code, ...) {
	va_list ap;

	if (code != NOERROR) {
		printf("Error %d: ", code);
	}

	va_start(ap, code);
	vprintf(msg, ap);
	va_end(ap);
	printf("\n");
}
#endif

#ifdef _DEBUG
static void _printf(const char *msg, ...) {
	char buf[512];		/* dangerous fixed size buffer */
	va_list ap;
	va_start(ap, msg);
	vsprintf(buf, msg, ap);
	va_end(ap);
	AddInLogMessageText("Debug: %s", NOERROR, buf);
}
#endif

/* Get the value of a server (CGI) variable as a string
 */
static int getVariable(struct jk_env *env, jk_ws_service_t *s, char *hdrName,
						 char *buf, DWORD bufsz, char **dest, const char *dflt) {
	int errID;
	private_ws_t *ws = (private_ws_t *) s->ws_private;

	if (ws->context->GetServerVariable(ws->context, hdrName, buf, bufsz, &errID)) {
		*dest = s->pool->pstrdup(env, s->pool, buf);
	} else {
		*dest = s->pool->pstrdup(env, s->pool, dflt);
	}

	/* env->l->jkLog(env, env->l, JK_LOG_DEBUG, "%s=%s\n", hdrName, *dest); */

	return JK_TRUE;
}

/* Get the value of a server (CGI) variable as an integer
 */
static int getVariableInt(struct jk_env *env, jk_ws_service_t *s, char *hdrName,
						char *buf, DWORD bufsz, int *dest, int dflt) {
	int errID;
	private_ws_t *ws = (private_ws_t *) s->ws_private;

	if (ws->context->GetServerVariable(ws->context, hdrName, buf, bufsz, &errID)) {
		*dest = atoi(buf);
	} else {
		*dest = dflt;
	}

	/* env->l->jkLog(env, env->l, JK_LOG_DEBUG, "%s=%d\n", hdrName, *dest); */

	return JK_TRUE;
}

/* Get the value of a server (CGI) variable as an integer
 */
static int getVariableBool(struct jk_env *env, jk_ws_service_t *s, char *hdrName,
							char *buf, DWORD bufsz, int *dest, int dflt) {
	int errID;
	private_ws_t *ws = (private_ws_t *) s->ws_private;

	if (ws->context->GetServerVariable(ws->context, hdrName, buf, bufsz, &errID)) {
		if (isdigit(buf[0])) {
			*dest = atoi(buf) != 0;
		} else if (noCaseStrCmp(buf, "yes") == 0 || noCaseStrCmp(buf, "on") == 0) {
			*dest = 1;
		} else {
			*dest = 0;
		}
	} else {
		*dest = dflt;
	}

	/* env->l->jkLog(env, env->l, JK_LOG_DEBUG, "%s=%d\n", hdrName, *dest); */

	return JK_TRUE;
}

/* A couple of utility macros to supply standard arguments to getVariable() and
 * getVariableInt().
 */
#define GETVARIABLE(name, dest, dflt)		\
	getVariable(env, s, (name), workBuf, sizeof(workBuf), (dest), (dflt))
#define GETVARIABLEINT(name, dest, dflt)	\
	getVariableInt(env, s, (name), workBuf, sizeof(workBuf), (dest), (dflt))
#define GETVARIABLEBOOL(name, dest, dflt)	\
	getVariableBool(env, s, (name), workBuf, sizeof(workBuf), (dest), (dflt))

/* Return 1 iff the supplied string contains "web-inf" (in any case
 * variation. We don't allow URIs containing web-inf, although
 * scanPath() actually looks for the string bounded by path punctuation
 * or the ends of the string, so web-inf must appear as a single element
 * of the supplied URI
 */
static int badURI(const char *uri) {
	static const char *wi = "web-inf";
	return scanPath(uri, strlen(uri), wi, strlen(wi));
}

/* Replacement for strcat() that updates a buffer pointer. It's
 * probably marginal, but this should be more efficient that strcat()
 * in cases where the string being concatenated to gets long because
 * strcat() has to count from start of the string each time.
 */
static void append(char **buf, const char *str) {
	int l = strlen(str);
	memcpy(*buf, str, l);
	(*buf)[l] = '\0';
	*buf += l;
}

/* Allocate space for a string given a start pointer and an end pointer
 * and return a pointer to the allocated, copied string.
 */
static char *subStr(jk_env_t *env, jk_pool_t *pool, const char *start, const char *end) {
	char *out = NULL;

	if (start != NULL && end != NULL && end > start) {
		int len = end - start;
		if (out = pool->alloc(env, pool, len + 1), NULL != out) {
			memcpy(out, start, len);
			out[len] = '\0';
		}
	}
	return out;
}

/* Like subStr() but use a static buffer if possible.
 */
static char *smartSubStr(jk_env_t *env, jk_pool_t *pool, char **bufp, int *bufSz,
							const char *start, const char *end) {
	int len = end - start;
	if (len < *bufSz) {
		char *rv = *bufp;
		memcpy(rv, start, len);
		rv[len++] = '\0';
		/* Adjust buffer pointer, length */
		*bufp  += len;
		*bufSz -= len;
		return rv;
	} else {
		return subStr(env, pool, start, end);
	}
}

static int JK_METHOD cbInit(struct jk_env *env, jk_ws_service_t *s,
							struct jk_worker *w, void *context) {
	return JK_TRUE;
}

/* Post request cleanup.
 */
static void JK_METHOD cbAfterRequest( struct jk_env *env, jk_ws_service_t *_this) {

}

/* Set the response head in the server structures. This will be called
 * before the first write.
 */
static int JK_METHOD cbHead(struct jk_env *env, jk_ws_service_t *s) {
	/* env->l->jkLog(env, env->l, JK_LOG_DEBUG, "Into jk_ws_service_t::cbHead\n"); */

	if (s->status < 100 || s->status >= 1000) {
		env->l->jkLog(env, env->l, JK_LOG_ERROR, "jk_ws_service_t::cbHead, invalid status %d\n", s->status);
		return JK_ERR;
	}

	if (s && s->ws_private) {
		private_ws_t *p = s->ws_private;

		if (!p->responseStarted) {
			char *hdrBuf;
			FilterResponseHeaders frh;
			int rc, errID;
			int hdrCount;
			const char *reason;

			p->responseStarted = JK_TRUE;

			reason = (NULL == s->msg) ? "" : s->msg;
			hdrCount = s->headers_out->size(env, s->headers_out);

			/* Build a single string containing all the headers
			 * because that's what Domino needs.
			 */
			if (hdrCount > 0) {
				int i;
				int hdrLen;
				char *bufp;

				for (i = 0, hdrLen = 3; i < hdrCount; i++) {
					hdrLen += strlen(s->headers_out->nameAt(env, s->headers_out, i));
					hdrLen += strlen(s->headers_out->valueAt(env, s->headers_out, i));
					hdrLen += 4;
				}

				hdrBuf = s->pool->alloc(env, s->pool, hdrLen);
				bufp = hdrBuf;

				for (i = 0; i < hdrCount; i++) {
					append(&bufp, s->headers_out->nameAt(env, s->headers_out, i));
					append(&bufp, ": ");
					append(&bufp, s->headers_out->valueAt(env, s->headers_out, i));
					append(&bufp, crlf);
				}

				append(&bufp, crlf);
			} else {
				hdrBuf = (char *) crlf;
			}

			frh.responseCode	= s->status;
			frh.reasonText		= (char *) reason;
			frh.headerText		= hdrBuf;

			/* Send the headers */
			rc = p->context->ServerSupport(p->context, kWriteResponseHeaders, &frh, NULL, 0, &errID);
		}
		return JK_OK;
	}

	env->l->jkLog(env, env->l, JK_LOG_ERROR, "jk_ws_service_t::cbHead, NULL parameters\n");

	return JK_ERR;
}

/*
 * Read a chunk of the request body into a buffer.  Attempt to read len
 * bytes into the buffer.  Write the number of bytes actually read into
 * nRead.  
 */
static int JK_METHOD cbRead(struct jk_env *env, jk_ws_service_t *s,
							  void *bytes, unsigned len, unsigned *nRead) {

	if (s && s->ws_private && bytes && nRead) {
		private_ws_t *p = s->ws_private;

		/* Copy data from Domino's buffer. Although it seems slightly
		 * improbably we're believing that Domino always buffers the
		 * entire request in memory. Not properly tested yet.
		 */
		if (len > p->reqSize) {
			len = p->reqSize;
		}
		memcpy(bytes, p->reqBuffer, len);
		p->reqBuffer += len;
		p->reqSize -= len;
		*nRead = len;

		return JK_OK;
	}

	env->l->jkLog(env, env->l, JK_LOG_ERROR, "jk_ws_service_t::Read, NULL parameters\n");

	return JK_ERR;
}

/*
 * Write a chunk of response data back to the browser.
 */
static int JK_METHOD cbWrite(struct jk_env *env, jk_ws_service_t *s,
							 const void *bytes, unsigned len) {

	/* env->l->jkLog(env, env->l, JK_LOG_DEBUG, "Into jk_ws_service_t::Write\n"); */

	if (s && s->ws_private && bytes) {
		private_ws_t *p = s->ws_private;
		int errID, rc;

		/* Send the data */
		if (len > 0) {
			rc = p->context->WriteClient(p->context, (char *) bytes, len, 0, &errID);
		}

		return JK_OK;
	}

	env->l->jkLog(env, env->l, JK_LOG_ERROR, "jk_ws_service_t::Write, NULL parameters\n");

	return JK_ERR;
}

/*
 * Flush the output buffers.
 */
static int JK_METHOD cbFlush(struct jk_env *env, jk_ws_service_t *s) {
	return JK_OK;
}

/* Return TRUE iff the specified filename is absolute. Note that this is only
 * called in cases where the definition of 'absolute' is not security sensitive. I'm
 * sure there are ways of constructing absolute Win32 paths that it doesn't catch.
 */
static int isAbsolutePath(const char *fn) {
#ifdef WIN32
	return fn[0] == '\\' || (isalpha(fn[0]) && fn[1] == ':');
#else
	return fn[0] == '/';
#endif
}

static const char *makeAbsolutePath(jk_env_t *env, const char *base, const char *name) {
	if (base == NULL || isAbsolutePath(name)) {
		return name;
	} else {
		int bsz = strlen(base);
		int nsz = strlen(name);
		int ads = (base[bsz-1] != PATHSEP) ? 1 : 0;
		char *buf;

		if (buf = workerEnv->pool->alloc(env, workerEnv->pool, bsz + ads + nsz + 1), NULL == buf) {
			return NULL;
		}
		memcpy(buf, base, bsz);
		if (ads) {
			buf[bsz] = PATHSEP;
		}
		memcpy(buf + bsz + ads, name, nsz);
		buf[bsz + ads + nsz] = '\0';

		return buf;
	}
}

#ifdef WIN32
static const char *readRegistry(jk_env_t *env, HKEY hkey, const char *key, const char *base) {
	DWORD type = 0;
	DWORD sz = 0;
	LONG rc;
	char *val;

	rc = RegQueryValueEx(hkey, key, (LPDWORD) 0, &type, NULL, &sz);
	if (rc != ERROR_SUCCESS || type != REG_SZ) {
		return NULL;
	}

	if (val = workerEnv->pool->alloc(env, workerEnv->pool, sz), NULL == val) {
		return NULL;
	}

	rc = RegQueryValueEx(hkey, key, (LPDWORD) 0, &type, val, &sz);
	if (rc == ERROR_SUCCESS) {
		return makeAbsolutePath(env, base, val);
	}

	return NULL;
}
#endif

static int readFromRegistry(jk_env_t *env) {
#ifdef WIN32
	HKEY hkey;
	long rc;
	const char *timeout;

	rc = RegOpenKeyEx(HKEY_LOCAL_MACHINE, REGISTRY_LOCATION, (DWORD) 0, KEY_READ, &hkey);
	if (ERROR_SUCCESS != rc) {
		return JK_FALSE;
	}

	serverRoot	= readRegistry(env, hkey, SERVER_ROOT_TAG,		NULL);
	workersFile	= readRegistry(env, hkey, WORKER_FILE_TAG,		serverRoot);
	tomcatStart	= readRegistry(env, hkey, TOMCAT_START_TAG,		serverRoot);
	tomcatStop	= readRegistry(env, hkey, TOMCAT_STOP_TAG,		serverRoot);
	timeout		= readRegistry(env, hkey, TOMCAT_TIMEOUT_TAG,	NULL);
	if (timeout != NULL) {
		tomcatTimeout = atoi(timeout);
	}

	RegCloseKey(hkey);

    iniFileUsed = JK_FALSE;

    return	NULL != serverRoot &&
			NULL != workersFile;

#else
	return JK_FALSE;
#endif
}

/* Read an entry from a map and return a newly allocated copy of it
 * on success or NULL on failure.
 */
static const char *readMap(jk_env_t *env, jk_map_t *map, const char *name, const char *base)
{
	const char *s = map->get(env, map, name);
	if (s) {
		return makeAbsolutePath(env, base, workerEnv->pool->pstrdup(env, workerEnv->pool, s));
	}
	return NULL;
}

/* Read parameters from an ini file or the registry
 */
static int readConfigData(jk_env_t *env) {
    jk_map_t *map;

	/* Attempt to read from an ini file */
    if (JK_OK == jk2_map_default_create(env, &map, workerEnv->pool )) {
        if (JK_OK == jk2_config_file_read(env, map, iniFileName)) {
			const char *timeout;

			serverRoot	= readMap(env, map, SERVER_ROOT_TAG,	NULL);
			workersFile = readMap(env, map, WORKER_FILE_TAG,	serverRoot);
			tomcatStart = readMap(env, map, TOMCAT_START_TAG,	serverRoot);
			tomcatStop	= readMap(env, map, TOMCAT_STOP_TAG,	serverRoot);
			timeout		= readMap(env, map, TOMCAT_TIMEOUT_TAG,	NULL);

			if (timeout != NULL) {
				tomcatTimeout = atoi(timeout);
			}

            iniFileUsed = JK_TRUE;
            return	NULL != serverRoot &&
					NULL != workersFile;
        }
    } else {
        env->l->jkLog(env, env->l, JK_LOG_ERROR,
               "read_registry_init_data, Failed to create map \n");
    }

	return readFromRegistry(env);
}

/* Send a simple response. Used when we don't want to bother Tomcat,
 * which in practice means for various error conditions that we can
 * detect internally.
 */
static void simpleResponse(FilterContext *context, int status, char *reason, char *body) {
	FilterResponseHeaders frh;
	int rc, errID;
	char hdrBuf[35];

	sprintf(hdrBuf, "Content-type: text/html%s%s", crlf, crlf);

	frh.responseCode	= status;
	frh.reasonText		= reason;
	frh.headerText		= hdrBuf;

	rc = context->ServerSupport(context, kWriteResponseHeaders, &frh, NULL, 0, &errID);
	rc = context->WriteClient(context, body, strlen(body), 0, &errID);
}

/* Called to reject a URI that contains the string "web-inf". We block
 * these because they may indicate an attempt to invoke arbitrary code.
 */
static unsigned int rejectBadURI(FilterContext *context) {
	static char *msg = "<html><body><h1>Access is Forbidden</h1></body></html>";

	simpleResponse(context, 403, "Forbidden", msg);

	return kFilterHandledRequest;
}

/* Called to generate a generic error response.
 */
static unsigned int rejectWithError(FilterContext *context) {
	static char *msg = "<html><body><h1>Error in Filter</h1></body></html>";

	simpleResponse(context, 500, "Error in Filter", msg);

	return kFilterHandledRequest;
}

/* Given all the HTTP headers as a single string parse them into individual
 * name, value pairs.
 */
static int parseHeaders(jk_env_t *env, jk_ws_service_t *s, const char *hdrs, int hdrSz) {
	int hdrCount = 0;
	const char *limit = hdrs + hdrSz;
	const char *name, *nameEnd;
	const char *value, *valueEnd;
	int gotContentLength = JK_FALSE;
	char buf[256];		/* Static buffer used for headers that are short enough to fit
						 * in it. A dynamic buffer is used for any longer headers.
						 */

	while (hdrs < limit) {
		/* buf is re-used for each header */
		char *bufp = buf;
		char *hdrName, *hdrValue;
		int sz = sizeof(buf);

		/* Skip line *before* doing anything, cos we want to lose the first line which
		 * contains the request. This code also moves to the next line after each header.
		 */
		while (hdrs < limit && (*hdrs != '\n' && *hdrs != '\r')) {
			hdrs++;
		}

		while (hdrs < limit && (*hdrs == '\n' || *hdrs == '\r')) {
			hdrs++;
		}

		if (hdrs >= limit) {
			break;
		}

		name = nameEnd = value = valueEnd = NULL;
		name = hdrs;
		while (hdrs < limit && *hdrs >= ' ' && *hdrs != ':') {
			hdrs++;
		}

		nameEnd = hdrs;
		if (hdrs < limit && *hdrs == ':') {
			hdrs++;
			while (hdrs < limit && (*hdrs == ' ' || *hdrs == '\t')) {
				hdrs++;
			}
			value = hdrs;
			while (hdrs < limit && *hdrs >= ' ') {
				hdrs++;
			}
			valueEnd = hdrs;
		}

		hdrName		= smartSubStr(env, s->pool, &bufp, &sz, name, nameEnd);
		/* Need to strdup the value because map->put doesn't for some reason */
		hdrValue	= subStr(env, s->pool, value, valueEnd);

        s->headers_in->put(env, s->headers_in, hdrName, hdrValue, NULL);

		gotContentLength |= (noCaseStrCmp(hdrName, CONTENT_LENGTH) == 0);

		hdrCount++;
	}

	/* Add a zero length content-length header if none was found in the 
	 * request.
	 */
	if (!gotContentLength) {
		s->headers_in->put(env, s->headers_in, CONTENT_LENGTH, "0", NULL);
		hdrCount++;
	}

	return hdrCount;
}

/* Initialize the service structure
 */
static int processRequest(struct jk_env *env, jk_ws_service_t *s,
					struct jk_worker *w, FilterRequest *fr) {
	/* This is the only fixed size buffer left. It won't be overflowed
	 * because the Domino API that reads into the buffer accepts a length
	 * constraint, and it's unlikely ever to be exhausted because the
	 * strings being will typically be short, but it's still aesthetically
	 * troublesome.
	 */
	char workBuf[16 * 1024];
	private_ws_t *ws = (private_ws_t *) s->ws_private;
	char *hdrs;
	int hdrsz;
	int errID = 0;
	int hdrCount;

	static char *methodName[] = { "", "HEAD", "GET", "POST", "PUT", "DELETE" };

	/* env->l->jkLog(env, env->l, JK_LOG_DEBUG, "processRequest(), s = %p, ws = %p\n", s, ws); */

	s->jvm_route = NULL;

	GETVARIABLE("AUTH_TYPE", &s->auth_type, "");
	GETVARIABLE("REMOTE_USER", &s->remote_user, "");
	GETVARIABLE("SERVER_PROTOCOL", &s->protocol, "");
	GETVARIABLE("REMOTE_HOST", &s->remote_host, "");
	GETVARIABLE("REMOTE_ADDR", &s->remote_addr, "");
	GETVARIABLE("SERVER_NAME", &s->server_name, "");
	GETVARIABLEINT("SERVER_PORT", &s->server_port, 80);
	GETVARIABLE("SERVER_SOFTWARE", &s->server_software, SERVERDFLT);
	GETVARIABLEINT("CONTENT_LENGTH", &s->content_length, 0);

	/* SSL Support
	 */
	GETVARIABLEBOOL("HTTPS", &s->is_ssl, 0);

	if (ws->reqData->requestMethod < 0 ||
		ws->reqData->requestMethod >= sizeof(methodName) / sizeof(methodName[0])) {
		return JK_ERR;
	}

	s->method			= methodName[ws->reqData->requestMethod];
	s->ssl_cert_len		= fr->clientCertLen;
	s->ssl_cert			= fr->clientCert;
	s->ssl_cipher		= NULL;
	s->ssl_session		= NULL;
	s->ssl_key_size		= -1;

    if (JK_OK != jk2_map_default_create(env, &s->headers_out, s->pool)) {
        env->l->jkLog(env, env->l, JK_LOG_ERROR, "jk_ws_service_t::init, Failed to create headers_out map\n");
        return JK_ERR;
    }

    if (JK_OK != jk2_map_default_create(env, &s->attributes, s->pool)) {
        env->l->jkLog(env, env->l, JK_LOG_ERROR, "jk_ws_service_t::init, Failed to create attributes map\n");
        return JK_ERR;
    }

    if (JK_OK != jk2_map_default_create(env, &s->headers_in, s->pool)) {
        env->l->jkLog(env, env->l, JK_LOG_ERROR, "jk_ws_service_t::init, Failed to create headers_in map\n");
        return JK_ERR;
    }

	if (s->is_ssl) {
		int i, dummy;

		/* It seems that Domino doesn't actually expose many of these but we live in hope.
		 */
		char *sslNames[] = {
			"CERT_ISSUER", "CERT_SUBJECT", "CERT_COOKIE", "CERT_FLAGS", "CERT_SERIALNUMBER",
			"HTTPS_SERVER_SUBJECT", "HTTPS_SECRETKEYSIZE", "HTTPS_SERVER_ISSUER", "HTTPS_KEYSIZE"
		};

		/* env->l->jkLog(env, env->l, JK_LOG_DEBUG, "Request is SSL\n"); */

		/* Read the variable into a dummy variable: we do this for the side effect of
		 * reading it into workBuf.
		 */
		GETVARIABLEINT("HTTPS_KEYSIZE", &dummy, 0);
		if (workBuf[0] == '[') {
			s->ssl_key_size = atoi(workBuf+1);
		}

		/* Should also try to make suitable values for s->ssl_cipher and
		 * s->ssl_session
		 */
		for (i = 0; i < sizeof(sslNames) / sizeof(sslNames[0]); i++) {
			char *value = NULL;
			GETVARIABLE(sslNames[i], &value, NULL);
			if (value) {
				s->attributes->put(env, s->attributes, sslNames[i], value, NULL);
			}
		}
	}

	/* Duplicate all the headers now
	 */
	hdrsz = ws->reqData->GetAllHeaders(ws->context, &hdrs, &errID);
	if (0 == hdrsz) {
		return JK_ERR;
	}

	hdrCount = parseHeaders(env, s, hdrs, hdrsz);

	return JK_OK;
}

/* Handle an HTTP request. Works out whether Tomcat will be interested then either
 * despatches it to Tomcat or passes it back to Domino.
 */
static unsigned int parsedRequest(FilterContext *context, FilterParsedRequest *reqData) {
	unsigned int errID;
	int rc;
	FilterRequest fr;
	int result = kFilterNotHandled;

	/* TODO: presumably this return code should be checked */
	rc = context->GetRequest(context, &fr, &errID);

	if (NONBLANK(fr.URL)) {
		char *uri = fr.URL;
		char *qp, *turi;
	    jk_uriEnv_t *uriEnv = NULL;
		int errID;
		char buf[256];	/* enough for the server's name */
		char *serverName;
		size_t serverNameSz;
		int serverPort;
		char *uriBuf;
		size_t uriSz, uriBufSz;
	    jk_env_t *env = workerEnv->globalEnv->getEnv(workerEnv->globalEnv);

		/* env->l->jkLog(env, env->l, JK_LOG_DEBUG, "parsedRequest() - %s\n", uri); */

		if (!context->GetServerVariable(context, "SERVER_PORT", buf, sizeof(buf), &errID)) {
			return rejectWithError(context);
		}

		serverPort = atoi(buf);

		if (!context->GetServerVariable(context, "SERVER_NAME", buf, sizeof(buf), &errID)) {
			return rejectWithError(context);
		}

		serverName = buf;	/* note serverName just aliases buf
							 * and will be destroyed if buf is reused
							 */

		serverNameSz = strlen(serverName) + 1;

		uriBuf   = serverName + serverNameSz;
		uriBufSz = sizeof(buf) - serverNameSz;
		uriSz    = strlen(uri) + 1;

		/* Use the stack buffer for sufficiently short URIs */
		if (uriSz <= uriBufSz) {
			turi = uriBuf;
		} else {
			turi = context->AllocMem(context, uriSz, 0, &errID);
		}
		memcpy(turi, uri, uriSz);
	
        rc = jk_requtil_unescapeUrl(turi);
		if (rc < 0) {
			return rejectWithError(context);
		}

        jk_requtil_getParents(turi);
        if (qp = strchr(turi, '?'), qp != NULL) {
			*qp++ = '\0';
		}

		if (badURI(turi)) {
			return rejectBadURI(context);
		}

        uriEnv = workerEnv->uriMap->mapUri(env, workerEnv->uriMap, serverName, serverPort, turi);
        
		if (NULL != uriEnv) {
			// Here we go
			private_ws_t ws;
			jk_ws_service_t s;
		    jk_pool_t *rPool = NULL;
		    jk_worker_t *worker = uriEnv->worker;

			rPool = worker->rPoolCache->get(env, worker->rPoolCache);
			if (NULL == rPool) {
				rPool = worker->mbean->pool->create(env, worker->mbean->pool, HUGE_POOL_SIZE);
				/* env->l->jkLog(env, env->l, JK_LOG_DEBUG, "HttpExtensionProc: new rpool\n"); */
			}

			jk2_requtil_initRequest(env, &s);

			s.pool					= rPool;
			s.is_recoverable_error	= JK_FALSE;
			s.response_started		= JK_FALSE;
			s.content_read			= 0;
			s.ws_private			= &ws;
			s.workerEnv				= workerEnv;
			s.head					= cbHead;
			s.read					= cbRead;
			s.write					= cbWrite;
			s.init					= cbInit;			/* never seems to be used */
			s.afterRequest			= cbAfterRequest;

            if (workerEnv->options == JK_OPT_FWDURICOMPATUNPARSED) {
				s.req_uri = env->globalPool->pstrdup(env, rPool, libFileName);
				/* Find the query string again in the original URI */
				if (qp = strchr(s.req_uri, '?'), NULL != qp) {
					*qp++ = '\0';
				}
            } else if (workerEnv->options == JK_OPT_FWDURIESCAPED) {
				/* Nasty static buffer */
				char euri[256];
				if (jk_requtil_escapeUrl(turi, buf, sizeof(euri))) {
					turi = euri;
				}
				s.req_uri = turi;
            } else {
				s.req_uri = turi;
            }

			s.query_string		= qp;

			/* Init our private structure
			 */
			ws.responseStarted	= JK_FALSE;
			ws.context			= context;
			ws.reqData			= reqData;

			/* Fetch info about the request
			 */
			ws.reqSize			= context->GetRequestContents(context, &ws.reqBuffer, &errID);

			rc = processRequest(env, &s, worker, &fr);
			if (JK_OK == rc) {
				rc = worker->service(env, uriEnv->worker, &s);
			}

			if (JK_OK == rc) {
				result = kFilterHandledRequest;
				/* env->l->jkLog(env, env->l, JK_LOG_DEBUG, "HttpExtensionProc service() returned OK\n"); */
			} else {
				result = kFilterError;
				env->l->jkLog(env, env->l, JK_LOG_ERROR, "HttpExtensionProc error, service() failed\n");
			}

			rPool->reset(env, rPool);
			rc = worker->rPoolCache->put(env, worker->rPoolCache, rPool);
		}

		workerEnv->globalEnv->releaseEnv(workerEnv->globalEnv, env);
	}

	return result;
}

static int runProg(const char *cmd) {
#ifdef WIN32
    STARTUPINFO si;
    PROCESS_INFORMATION pi;

	memset(&si, 0, sizeof(si));
    si.cb			= sizeof(si);    // Start the child process.
	si.dwFlags		= STARTF_USESHOWWINDOW;
	si.wShowWindow	= SW_SHOWMAXIMIZED;

	if (!CreateProcess(NULL, (char *) cmd, NULL, NULL, FALSE, 0, NULL, NULL, &si, &pi))	{
		DWORD err = GetLastError();
		AddInLogMessageText("Command \"%s\" failed (error %u)", NOERROR, cmd, err);
		return JK_FALSE;
	}

	if (WAIT_OBJECT_0 == WaitForSingleObject(pi.hProcess, tomcatTimeout)) {
		return JK_TRUE;
	}

	AddInLogMessageText("Command \"%s\" didn't complete in time", NOERROR, cmd);
	return JK_FALSE;
#else
	int err = system(cmd);
	if (0 == err) {
		return 1;
	}
	AddInLogMessageText("Command \"%s\" failed (error %d)", NOERROR, cmd, err);
	return 0;
#endif
}

/* Main entry point for the filter. Called by Domino for every HTTP request.
 */
DLLEXPORT unsigned int HttpFilterProc(FilterContext *context, unsigned int eventType, void *eventData) {
	switch (eventType) {
	case kFilterParsedRequest:
		return parsedRequest(context, (FilterParsedRequest *) eventData);
	default:
		break;
	}

	return kFilterNotHandled;
}

/* Called when the filter is unloaded. Free various resources and
 * display a banner.
 */
DLLEXPORT unsigned int TerminateFilter(unsigned int reserved) {
    jk_env_t *env = workerEnv->globalEnv->getEnv(workerEnv->globalEnv);

	// TODO: Work out if we're doing everything we need to here
	if (isInited) {
        if (workerEnv) {
            jk_env_t *env = workerEnv->globalEnv;
            workerEnv->close(env, workerEnv);
        }

		isInited = JK_FALSE;
	}

#ifndef TESTING
	if (NONBLANK(tomcatStop)) {
		AddInLogMessageText("Attempting to stop Tomcat: %s", NOERROR, tomcatStop);
		runProg(tomcatStop);
	}
#endif

	AddInLogMessageText(FILTERDESC " unloaded", NOERROR);

    apr_pool_destroy(jk_globalPool);

	return kFilterHandledEvent;
}

/* Called when Domino loads the filter. Reads a load of config data from
 * the registry and elsewhere and displays a banner.
 */
DLLEXPORT unsigned int FilterInit(FilterInitData *filterInitData) {
    jk_logger_t *l;
    jk_pool_t *globalPool;
    jk_bean_t *jkb;
    jk_env_t *env;

#ifndef WIN32
	Dl_info info;
	char *dot, *slash;

	memset(&info, 0, sizeof(info));
	if (!dladdr(FilterInit, &info) || NULL == info.dli_fname) {
		goto initFailed;
	}

	lstrcpyn(libFileName, info.dli_fname, sizeof(libFileName)-1);
	lstrcpyn(iniFileName, info.dli_fname, sizeof(libFileName)-1);
	slash = strrchr(iniFileName, PATHSEP);
	if (NULL == slash) {
		slash = iniFileName;
	} else {
		slash++;
	}
	dot = strrchr(slash, '.');
	if (NULL == dot) {
		dot = slash + strlen(slash);
	}
	lstrcpyn(slash, PROPERTIES_EXT, (iniFileName + sizeof(iniFileName) - 1) - slash);
#endif

    apr_initialize();
    apr_pool_create(&jk_globalPool, NULL);

    jk2_pool_apr_create(NULL, &globalPool, NULL, jk_globalPool);

    /* Create the global environment. This will register the default
     * factories
     */
    env = jk2_env_getEnv(NULL, globalPool);

#ifdef TESTING
    env->registerFactory(env, "logger.printf", jk2_logger_printf_factory);
#endif

    jkb = env->createBean2(env, env->globalPool, LOGGER, "");
    env->alias(env, LOGGER ":", "logger");
    l = jkb->object;

    env->l = l;
    env->soName = env->globalPool->pstrdup(env, env->globalPool, libFileName);

    if (NULL == env->soName) {
        env->l->jkLog(env, env->l, JK_LOG_ERROR, "Error creating env->soName\n");
		goto initFailed;
    }

	/* Initialise logger
	 */
    env->l->init(env, env->l);

    /* Create the workerEnv
	 */
    jkb = env->createBean2(env, env->globalPool,"workerEnv", "");
    workerEnv = jkb->object;
    env->alias(env, "workerEnv:", "workerEnv");

    if (NULL == workerEnv) {
        env->l->jkLog(env, env->l, JK_LOG_ERROR, "Error creating workerEnv\n");
        goto initFailed;
    }

    workerEnv->childId = 0;

	if (!readConfigData(env)) {
		goto initFailed;
	}

    workerEnv->initData->add(env, workerEnv->initData, "serverRoot",
                             workerEnv->pool->pstrdup(env, workerEnv->pool, serverRoot));

    env->l->jkLog(env, env->l, JK_LOG_INFO, "Set serverRoot %s\n", serverRoot);

	/* Note: we cast away the const qualifier on workersFile here
	 */
	if (JK_OK != workerEnv->config->setPropertyString(env, workerEnv->config,
														"config.file", (char *) workersFile)) {
		goto initFailed;
	}

    workerEnv->init(env, workerEnv);

#ifndef TESTING
	/* Attempt to launch Tomcat
	 */
	if (NONBLANK(tomcatStart)) {
		AddInLogMessageText("Attempting to start Tomcat: %s", NOERROR, tomcatStart);
		runProg(tomcatStart);
	}
#endif

	filterInitData->appFilterVersion	= kInterfaceVersion;
	filterInitData->eventFlags			= kFilterParsedRequest;

	lstrcpyn(filterInitData->filterDesc, FILTERDESC, sizeof(filterInitData->filterDesc));
	isInited = JK_TRUE;

	/* Display banner
	 */
	AddInLogMessageText("%s loaded", NOERROR, filterInitData->filterDesc);

	return kFilterHandledEvent;

initFailed:
	AddInLogMessageText("Error loading %s", NOERROR, FILTERDESC);
	return kFilterError;
}

#ifdef WIN32
/* Handle DLL initialisation (on WIN32) by working out what the INI file
 * should be called.
 */
BOOL WINAPI DllMain(HINSTANCE hInst, ULONG ulReason, LPVOID lpReserved) {
	BOOL fReturn = TRUE;
	char drive[_MAX_DRIVE];
	char dir[_MAX_DIR];
	char fname[_MAX_FNAME];

	switch (ulReason) {
	case DLL_PROCESS_ATTACH:
		if (GetModuleFileName(hInst, libFileName, sizeof(libFileName))) {
			_splitpath(libFileName, drive, dir, fname, NULL);
			_makepath(iniFileName, drive, dir, fname, PROPERTIES_EXT);
		} else {
			fReturn = FALSE;
		}
		break;
	default:
		break;
	}
	return fReturn;
}
#endif

#ifdef TESTING
/* Handle initialisation in the test harness environment.
 */
void TestMain(void) {
	strcpy(libFileName, "test.exe");
	strcpy(iniFileName, "test" PROPERTIES_EXT);
}
#endif
