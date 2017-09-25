/* ========================================================================= *
 *                                                                           *
 *                 The Apache Software License,  Version 1.1                 *
 *                                                                           *
 *          Copyright (c) 1999-2003 The Apache Software Foundation.          *
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

/**
 * Utils for processing various request components
 *
 * @author: Gal Shachor <shachor@il.ibm.com>                           
 * @author: Henri Gomez <hgomez@apache.org>
 * @author: Costin Manolache
 */

#ifndef JK_REQUTIL_H
#define JK_REQUTIL_H

#include "jk_global.h"
#include "jk_channel.h"
#include "jk_env.h"

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

/*
 * Frequent request headers, these headers are coded as numbers
 * instead of strings.
 */
#define SC_ACCEPT               (unsigned short)0xA001
#define SC_ACCEPT_CHARSET       (unsigned short)0xA002
#define SC_ACCEPT_ENCODING      (unsigned short)0xA003
#define SC_ACCEPT_LANGUAGE      (unsigned short)0xA004
#define SC_AUTHORIZATION        (unsigned short)0xA005
#define SC_CONNECTION           (unsigned short)0xA006
#define SC_CONTENT_TYPE         (unsigned short)0xA007
#define SC_CONTENT_LENGTH       (unsigned short)0xA008
#define SC_COOKIE               (unsigned short)0xA009    
#define SC_COOKIE2              (unsigned short)0xA00A
#define SC_HOST                 (unsigned short)0xA00B
#define SC_PRAGMA               (unsigned short)0xA00C
#define SC_REFERER              (unsigned short)0xA00D
#define SC_USER_AGENT           (unsigned short)0xA00E

/*
 * Frequent response headers, these headers are coded as numbers
 * instead of strings.
 * 
 * Content-Type
 * Content-Language
 * Content-Length
 * Date
 * Last-Modified
 * Location
 * Set-Cookie
 * Servlet-Engine
 * Status
 * WWW-Authenticate
 * 
 */
#define SC_RESP_CONTENT_TYPE        (unsigned short)0xA001
#define SC_RESP_CONTENT_LANGUAGE    (unsigned short)0xA002
#define SC_RESP_CONTENT_LENGTH      (unsigned short)0xA003
#define SC_RESP_DATE                (unsigned short)0xA004
#define SC_RESP_LAST_MODIFIED       (unsigned short)0xA005
#define SC_RESP_LOCATION            (unsigned short)0xA006
#define SC_RESP_SET_COOKIE          (unsigned short)0xA007
#define SC_RESP_SET_COOKIE2         (unsigned short)0xA008
#define SC_RESP_SERVLET_ENGINE      (unsigned short)0xA009
#define SC_RESP_STATUS              (unsigned short)0xA00A
#define SC_RESP_WWW_AUTHENTICATE    (unsigned short)0xA00B
#define SC_RES_HEADERS_NUM          11




/** Get header value using a lookup table. 
 */
const char *jk2_requtil_getHeaderById(struct jk_env *env, int sc);

/**
 * Get method id. 
 */
int jk2_requtil_getMethodId(struct jk_env *env, const char    *method,
                           unsigned char *sc);

/**
 * Get header id.
 */
int  jk2_requtil_getHeaderId(struct jk_env *env, const char *header_name,
                            unsigned short *sc);

/** Retrieve session id from the cookie or the parameter                      
 * (parameter first)
 */
char *jk2_requtil_getSessionId(struct jk_env *env, jk_ws_service_t *s);

/** Retrieve the cookie with the given name
 */
char *jk2_requtil_getCookieByName(struct jk_env *env, jk_ws_service_t *s,
                                 const char *name);

/* Retrieve the parameter with the given name
 */
char *jk2_requtil_getPathParam(struct jk_env *env, jk_ws_service_t *s, const char *name);


/** Extract the 'route' from the session id. The route is
 *  the id of the worker that generated the session and where all
 *  further requests in that session will be sent.
*/
char *jk2_requtil_getSessionRoute(struct jk_env *env, jk_ws_service_t *s);


/** Initialize the request 
 * 
 * jk_init_ws_service
 */ 
void jk2_requtil_initRequest(struct jk_env *env, jk_ws_service_t *s);


int jk2_requtil_readFully(struct jk_env *env, jk_ws_service_t *s,
                         unsigned char   *buf,
                         unsigned         len);

int jk_requtil_escapeUrl(const char *path, char *dest, int destsize);

int jk_requtil_unescapeUrl(char *url);

int jk_requtil_uriIsWebInf(char *uri);

void jk_requtil_getParents(char *name);

/** return the size of the encoding of a certificate */
int jk_requtil_base64CertLen(int len);

/** Do a base-64 encoding of the certificate */
int jk_requtil_base64EncodeCert(char *encoded,
                                const unsigned char *string, int len);

int jk2_serialize_postHead(jk_env_t *env, jk_msg_t   *msg,
                           jk_ws_service_t  *r,
                           jk_endpoint_t *ae);

int jk2_serialize_request13(jk_env_t *env, jk_msg_t *msg,
                            jk_ws_service_t *s,
                            jk_endpoint_t *ae);



#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif 
