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
 * Description: Apache1.3 adapter 
 */

#include "httpd.h"
#include "http_config.h"
#include "http_request.h"
#include "http_core.h"
#include "http_protocol.h"
#include "http_main.h"
#include "http_log.h"

#include "util_script.h"
#include "util_date.h"
/*
 * Jakarta (jk_) include files
 */
#include "jk_global.h"
#include "jk_map.h"
#include "jk_pool.h"
#include "jk_env.h"
#include "jk_service.h"
#include "jk_worker.h"
#include "jk_workerEnv.h"
#include "jk_uriMap.h"
#include "jk_requtil.h"


#define NULL_FOR_EMPTY(x)   ((x && !strlen(x)) ? NULL : x) 

static int JK_METHOD jk2_service_apache13_head(jk_env_t *env, jk_ws_service_t *s )
{
    int h;
    int numheaders;
    request_rec *r;
    jk_map_t *headers;
    
    if(s==NULL ||  s->ws_private==NULL )
        return JK_ERR;
    
    r = (request_rec *)s->ws_private;  
        
    if(s->msg==NULL) {
        s->msg = "";
    }
    r->status = s->status;
    r->status_line = ap_psprintf(r->pool, "%d %s", s->status, s->msg);

    headers=s->headers_out;
    numheaders = headers->size(env, headers);
    /* XXX As soon as we switch to jk_map_apache13, this will not be needed ! */
    if( s->uriEnv->mbean->debug > 0 ) 
        env->l->jkLog(env, env->l, JK_LOG_INFO, 
                      "service.head() %d %d\n", s->status,
                      numheaders);
    
    for(h = 0 ; h < numheaders; h++) {
        char *name=headers->nameAt( env, headers, h );
        char *val=headers->valueAt( env, headers, h );

        if( s->uriEnv->mbean->debug > 0 ) 
            env->l->jkLog(env, env->l, JK_LOG_INFO, 
                          "service.head() %s: %s %d %d\n",
                          name, val, h, headers->size( env, headers ));

        /* the cmp can also be avoided in we do this earlier and use
           the header id */
        if( strcasecmp(name, "Content-type") == 0 ) {
            /* XXX should be done in handler ! */
            char *tmp = ap_pstrdup(r->pool, val);
            ap_content_type_tolower(tmp); 
            r->content_type = tmp;
            ap_table_set(r->headers_out, name, val);
        } else if(strcasecmp(name, "Location") == 0 ) {
            /* XXX setn */
            ap_table_set(r->headers_out, name, val);
        } else if(strcasecmp(name, "Content-Length") == 0) {
            ap_table_set(r->headers_out, name, val );
        } else if(strcasecmp(name, "Transfer-Encoding") == 0 ) {
            ap_table_set(r->headers_out,name, val);
        } else if(strcasecmp(name, "Last-Modified") == 0 ) {
            /*
             * If the script gave us a Last-Modified header, we can't just
             * pass it on blindly because of restrictions on future values.
             */
            ap_update_mtime(r, ap_parseHTTPdate(val));
            ap_set_last_modified(r);
            ap_table_set(r->headers_out, name, val);
        } else {                
            ap_table_add(r->headers_out, name, val);
            /* ap_table_set(r->headers_out, name, val); */
        }
    }

    ap_send_http_header(r);
    s->response_started = JK_TRUE;
    
    return JK_OK;
}

/*
 * Read a chunk of the request body into a buffer.  Attempt to read len
 * bytes into the buffer.  Write the number of bytes actually read into
 * actually_read.
 *
 * Think of this function as a method of the apache1.3-specific subclass of
 * the jk_ws_service class.  Think of the *s param as a "this" or "self"
 * pointer.
 */
static int JK_METHOD jk2_service_apache13_read(jk_env_t *env, jk_ws_service_t *s,
                                              void *b, unsigned len,
                                              unsigned *actually_read)
{
    long rv;

    if(s==NULL  || s->ws_private==NULL  || b==NULL || actually_read==NULL ) {
        return JK_ERR;
    }

    if(!s->read_body_started) {
        if(ap_should_client_block(s->ws_private)) {
            s->read_body_started = JK_TRUE;
        }
    }
    rv = ap_get_client_block(s->ws_private, b, len);
    
    if( rv < 0) {
        *actually_read = 0;
    } else {
        *actually_read = (unsigned) rv;
    }
    return JK_OK;
} 

/*
 * Write a chunk of response data back to the browser.  If the headers
 * haven't yet been sent over, send over default header values (Status =
 * 200, basically).
 *
 * Write len bytes from buffer b.
 *
 * Think of this function as a method of the apache1.3-specific subclass of
 * the jk_ws_service class.  Think of the *s param as a "this" or "self"
 * pointer.
 */

/* Works with 4096, fails with 8192 */
#ifndef CHUNK_SIZE
#define CHUNK_SIZE 4096
#endif

static int JK_METHOD jk2_service_apache13_write(jk_env_t *env, jk_ws_service_t *s,
                                                const void *b, unsigned int len)
{
    int rc;
    
    if(s==NULL || s->ws_private == NULL || b==NULL )
        return JK_ERR;
    
    {
        size_t rd = 0;
        long ll=len;
        char *bb=(char *)b;
        request_rec *rr=s->ws_private;
        BUFF *bf = rr->connection->client;
        
        if(!s->response_started) {
            if( s->uriEnv->mbean->debug > 0 ) 
                env->l->jkLog(env, env->l, JK_LOG_INFO, 
                              "service.write() default head\n");

            rc=s->head(env, s);
            if( rc != JK_OK ) {
                return rc;
            }
        }
        if (rr->header_only) {
            ap_bflush(bf);
            return JK_OK;
        }
        
        /* Debug - try to get around rwrite */
        while( ll > 0 ) {
            unsigned long toSend=(ll>CHUNK_SIZE) ? CHUNK_SIZE : ll;
            rd = ap_rwrite((const char *)bb, toSend, rr );
            if( s->uriEnv->mbean->debug > 0 ) 
                env->l->jkLog(env, env->l, JK_LOG_INFO, 
                              "service.write()  %ld (%ld) out of %ld \n",toSend, rd, ll );
            ll-=CHUNK_SIZE;
            bb+=CHUNK_SIZE;
            
            if(toSend != rd) { 
                return JK_ERR; 
            } 
                
        }

        /*
         * To allow server push. After writing full buffers
         */
        ap_bflush(bf);
    }
    return JK_OK;
}

/* ========================================================================= */
/* Utility functions                                                         */
/* ========================================================================= */

static int jk2_get_content_length(jk_env_t *env, request_rec *r)
{
    if(r->clength > 0) {
        return r->clength;
    } else {
        char *lenp = (char *)ap_table_get(r->headers_in, "Content-Length");

        if(lenp) {
            int rc = atoi(lenp);
            if(rc > 0) {
                return rc;
            }
        }
    }

    return 0;
}

static int JK_METHOD jk2_init_ws_service(jk_env_t *env, jk_ws_service_t *s,
                               jk_worker_t *worker, void *serverObj)
{
    char *ssl_temp      = NULL;
    jk_workerEnv_t *workerEnv=worker->workerEnv;
    request_rec *r=serverObj;
    int need_content_length_header=JK_FALSE;

    /* Common initialization */
    /* XXX Probably not needed, we're duplicating */
    jk2_requtil_initRequest(env, s);
    
    s->ws_private = r;
    s->response_started = JK_FALSE;
    s->read_body_started = JK_FALSE;
    s->workerEnv=workerEnv;

    workerEnv->childId = r->connection->child_num;

    s->jvm_route        = NULL;    /* Used for sticky session routing */

    s->auth_type    = NULL_FOR_EMPTY(r->connection->ap_auth_type);
    s->remote_user  = NULL_FOR_EMPTY(r->connection->user);

    s->protocol     = r->protocol;
    s->remote_host  = (char *)ap_get_remote_host(r->connection,
                                                 r->per_dir_config, REMOTE_HOST);
    s->remote_host  = NULL_FOR_EMPTY(s->remote_host);
    s->remote_addr  = NULL_FOR_EMPTY(r->connection->remote_ip);

    /* get server name */
    s->server_name= (char *)(r->hostname ? r->hostname :
                 r->server->server_hostname);
    s->server_port = htons(r->connection->local_addr.sin_port);
    s->server_software = (char *)ap_get_server_version();

    s->method         = (char *)r->method;
    s->content_length = jk2_get_content_length(env, r);
    s->is_chunked     = r->read_chunked;
    s->no_more_chunks = 0;
    s->query_string   = r->args;

    /*
     * The 2.2 servlet spec errata says the uri from
     * HttpServletRequest.getRequestURI() should remain encoded.
     * [http://java.sun.com/products/servlet/errata_042700.html]
     *
     * We use JkOptions to determine which method to be used
     *
     * ap_escape_uri is the latest recommanded but require
     *               some java decoding (in TC 3.3 rc2)
     *
     * unparsed_uri is used for strict compliance with spec and
     *              old Tomcat (3.2.3 for example)
     *
     * uri is use for compatibilty with mod_rewrite with old Tomcats
     */

    switch (workerEnv->options & JK_OPT_FWDURIMASK) {

        case JK_OPT_FWDURICOMPATUNPARSED :
            s->req_uri      = r->unparsed_uri;
            if (s->req_uri != NULL) {
                char *query_str = strchr(s->req_uri, '?');
                if (query_str != NULL) {
                    *query_str = 0;
                }
            }

        break;

        case JK_OPT_FWDURICOMPAT :
            s->req_uri = r->uri;
        break;

        case JK_OPT_FWDURIESCAPED :
            s->req_uri      = ap_escape_uri(r->pool, r->uri);
        break;

        default :
            return JK_ERR;
    }

    s->is_ssl       = JK_FALSE;
    s->ssl_cert     = NULL;
    s->ssl_cert_len = 0;
    s->ssl_cipher   = NULL;        /* required by Servlet 2.3 Api, 
                                   allready in original ajp13 */
    s->ssl_session  = NULL;
    s->ssl_key_size = -1;        /* required by Servlet 2.3 Api, added in jtc */

    if(workerEnv->ssl_enable || workerEnv->envvars_in_use) {
        ap_add_common_vars(r);

        if(workerEnv->ssl_enable) {
            ssl_temp = 
                (char *)ap_table_get(r->subprocess_env, 
                                      workerEnv->https_indicator);
            if(ssl_temp && !strcasecmp(ssl_temp, "on")) {
                s->is_ssl       = JK_TRUE;
                s->ssl_cert     = 
                    (char *)ap_table_get(r->subprocess_env, 
                                          workerEnv->certs_indicator);
                if(s->ssl_cert) {
                    s->ssl_cert_len = strlen(s->ssl_cert);
                }
                /* Servlet 2.3 API */
                s->ssl_cipher   = 
                    (char *)ap_table_get(r->subprocess_env, 
                                          workerEnv->cipher_indicator);
                 s->ssl_session  = 
                    (char *)ap_table_get(r->subprocess_env, 
                                          workerEnv->session_indicator);

                if (workerEnv->options & JK_OPT_FWDKEYSIZE) {
                    /* Servlet 2.3 API */
                    ssl_temp = (char *)ap_table_get(r->subprocess_env, 
                                                 workerEnv->key_size_indicator);
                    if (ssl_temp)
                        s->ssl_key_size = atoi(ssl_temp);
                }
            }
        }

        jk2_map_default_create(env, &s->attributes, s->pool );
        
        if(workerEnv->envvars_in_use) {
            int envCnt=workerEnv->envvars->size( env, workerEnv->envvars );
            int i;

            for( i=0; i< envCnt ; i++ ) {
                char *name= workerEnv->envvars->nameAt( env, workerEnv->envvars, i );
                char *val= (char *)ap_table_get(r->subprocess_env, name);
                if(val==NULL) {
                    val=workerEnv->envvars->valueAt( env, workerEnv->envvars, i );
                }
                s->attributes->put( env, s->attributes, name, val, NULL );
            }
        }
    }

    jk2_map_default_create(env, &s->headers_in, s->pool );

    if(r->headers_in && ap_table_elts(r->headers_in)) {
        const array_header *t = ap_table_elts(r->headers_in);
        if(t && t->nelts) {
            int i;

            table_entry *elts = (table_entry *)t->elts;

            for(i = 0 ; i < t->nelts ; i++) {
                s->headers_in->add( env, s->headers_in,
                                    elts[i].key, elts[i].val);
            }
        }
    }

    if(!s->is_chunked && s->content_length == 0) {
        /* XXX if r->contentLength == 0 I assume there's no header
           or a header with '0'. In the second case, put will override it 
         */
        s->headers_in->put( env, s->headers_in, "content-length", "0", NULL );
    }

    jk2_map_default_create(env, &s->headers_out, s->pool );

    return JK_OK;
}

/*
 * If the servlet engine didn't consume all of the
 * request data, consume and discard all further
 * characters left to read from client
 *
 *  XXX Is it the right thing to do ????? Why spend the
 *  bandwith, the servlet decided not to read the POST then
 *  jk shouldn't do it instead, and the user should get the
 *  error message !
 */
static void JK_METHOD jk2_service_apache13_afterRequest(jk_env_t *env, jk_ws_service_t *s )
{
    
    if (s->content_read < s->content_length ||
        (s->is_chunked && ! s->no_more_chunks)) {
        
        request_rec *r=s->ws_private;

        char *buff = ap_palloc(r->pool, 2048);
        if (buff != NULL) {
            int rd;
            while ((rd = ap_get_client_block(r, buff, 2048)) > 0) {
                s->content_read += rd;
            }
        }
    }
}

int jk2_service_apache13_init(jk_env_t *env, jk_ws_service_t *s)
{
    if(s==NULL ) {
        return JK_ERR;
    }

    s->head   = jk2_service_apache13_head;
    s->read   = jk2_service_apache13_read;
    s->write  = jk2_service_apache13_write;
    s->init   = jk2_init_ws_service;
    s->afterRequest     = jk2_service_apache13_afterRequest;
    
    return JK_OK;
}
