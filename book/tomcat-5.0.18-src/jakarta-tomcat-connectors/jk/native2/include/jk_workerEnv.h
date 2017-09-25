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

/***************************************************************************
 * Description: Workers controller header file                             *
 * Author:      Gal Shachor <shachor@il.ibm.com>                           * 
 * Version:     $Revision: 1.29 $                                           *
 ***************************************************************************/

#ifndef JK_WORKERENV_H
#define JK_WORKERENV_H

#include "jk_logger.h"
#include "jk_endpoint.h"
#include "jk_config.h"
#include "jk_worker.h"
#include "jk_map.h"
#include "jk_uriMap.h"
#include "jk_uriEnv.h"
#include "jk_handler.h"
#include "jk_service.h"
#include "jk_shm.h"
#include "jk_vm.h"

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

struct jk_worker;
struct jk_channel;
struct jk_endpoint;
struct jk_env;
struct jk_config;
struct jk_uriMap;
struct jk_uriEnv;
struct jk_map;
struct jk_logger;
struct jk_handler;
struct jk_ws_service;

/* Temporary hardcoded handler IDs. Will be replaced with a name based dynamic mechanism */

/* Write a body chunk from the servlet container to the web server */
#define JK_HANDLE_AJP13_SEND_BODY_CHUNK    3

/* Send response headers from the servlet container to the web server. */
#define JK_HANDLE_AJP13_SEND_HEADERS       4

/* Marks the end of response. */
#define JK_HANDLE_AJP13_GET_BODY_CHUNK     6

/*  Marks the end of response. */
#define JK_HANDLE_AJP13_END_RESPONSE       5

/* Get a PONG reply from the servlet container. */
#define JK_HANDLE_AJP13_PONG_REPLY       9

/* Second Login Phase (servlet engine -> web server), md5 seed is received */
#define JK_HANDLE_LOGON_SEED	0x11

/* Login Accepted (servlet engine -> web server) */
#define JK_HANDLE_LOGON_OK	0x13

/* Login Rejected (servlet engine -> web server) */
#define JK_HANDLE_LOGON_ERR	0x14

/* Dispatcher for jni channel ( java->C ) */
#define JK_HANDLE_JNI_DISPATCH 0x15

/* Dispatcher for shm object ( java->C) */
#define JK_HANDLE_SHM_DISPATCH 0x16

/* Dispatcher for channel components ( java->C )*/
#define JK_HANDLE_CH_DISPATCH 0x17

/* Dispatcher for mutex object  ( java->C ) */
#define JK_HANDLE_MUTEX_DISPATCH 0x18

    
/*
 * Jk configuration and global methods. 
 * 
 * Env Information to be provided to worker at init time
 * With AJP14 support we need to have access to many informations
 * about web-server, uri to worker map....
 */
struct jk_workerEnv {
    struct jk_bean *mbean;

    /* Use this pool for all global settings
     */
    struct jk_pool *pool;
    
    /* Active workers hashtable. 
     */
    struct jk_map *worker_map;

    /* Channels
     */
    struct jk_map *channel_map;

    struct jk_map *endpointMap;

    /* In a multi-process server, like Apache, stores the child
       id in the scoreboard ( if the scoreboard is used ).
       This in turn is used in the jk scoreboard to store informations
       about each instance.
       If -1 - shm is disabled.
    */
    int childId;
    int childProcessId;
    int childGeneration;

    struct jk_env *globalEnv;

    /** Worker that will be used by default, if no other
        worker is specified. Usefull for SetHandler or
        to avoid the lookup
        XXX no need - lb is the default, easy to get it.
    */
    struct jk_worker *defaultWorker;
    
    /* Web-Server we're running on (Apache/IIS/NES).
     */
    char *server_name;
    
    /* XXX Virtual server handled - "*" is all virtual
     */
    char *virtual;

    /** Initialization properties, set via native options or workers.properties.
     */
    /* XXX renamed from init_data to force all code to use setProperty
       This is private property !
    */
    struct jk_map *initData;

    /*
     * Log options. XXX move it to uriEnv, make it configurable per webapp.
     * XXX What about apache native logger ?
     */
    char *logger_name;

    struct jk_uriMap *uriMap;

    int was_initialized;

    /*
     * SSL Support
     */
    int  ssl_enable;
    char *https_indicator;
    char *certs_indicator;
    char *cipher_indicator;
    char *session_indicator;  /* Servlet API 2.3 requirement */
    char *key_size_indicator; /* Servlet API 2.3 requirement */

    /*
     * Jk Options
     */
    int options;

    /** Old REUSE_WORKER define. Instead of using an object pool, use
        thread data to recycle the connection. */
    int perThreadWorker;
    
    /*
     * Environment variables support
     */
    int envvars_in_use;
    struct jk_map * envvars;

    struct jk_config *config;
    
    struct jk_shm *shm;

    /* Slot for endpoint stats
     */
    struct jk_shm_slot *epStat;
    
    /* Handlers. This is a dispatch table for messages, for
     * each message id we have an entry containing the jk_handler_t.
     * lastMessageId is the size of the table.
     */
    struct jk_handler **handlerTable;
    int lastMessageId;

    /* The vm - we support a single instance per process
     * ( i.e can't have both jdk1.1 and jdk1.2 at the same time,
     *  or 2 instances of the same vm. )
     */
    struct jk_vm *vm;
    
    /** Private data, associated with the 'real' server
     *  server_rec * in apache
     */
    void *_private;

    struct jk_mutex *cs;

    /* Global setting to enable counters on all requests.
     *  That adds about 2-3 ms per request ( at least on linux ),
     *  and will store average and max processing time per endpoint
     *  ( that can be agregated per worker or per server ).
     * Note that we can't collect per request times - it's not
     * thread safe and sync is expensive. As workaround you
     * can enable timers only on a specific request and/or
     * use a dedicated worker/channel for that request.
     */
    int timing;
    
    /* -------------------- Methods -------------------- */

    /* Register a callback handler, for methods from java to C
     */
    int (*registerHandler)(struct jk_env *env,
                           struct jk_workerEnv *_this,
                           const char *type, const char *name, int id,
                           jk_handler_callback callback,
                           char *signature);

    
    int (*addWorker)(struct jk_env *env,
                     struct jk_workerEnv *_this,
                     struct jk_worker *w);

    int (*addChannel)(struct jk_env *env,
                      struct jk_workerEnv *_this,
                      struct jk_channel *w);
    
    /** Add an endpoint. Endpoints are long lived and used to store
        statistics. The endpoint can be in used in only one thread
        at a time, it's a good way to avoid synchronization.
     */
    int (*addEndpoint)(struct jk_env *env, struct jk_workerEnv *wEnv,
                       struct jk_endpoint *ep);

    int (*initChannel)(struct jk_env *env,
                       struct jk_workerEnv *wEnv, struct jk_channel *ch);
    
    /** Call the handler associated with the message type.
     */
    int (*dispatch)(struct jk_env *env, struct jk_workerEnv *_this,
                    void *target, struct jk_endpoint *ep, int code, struct jk_msg *msg);

    /** Utility method for stream-based workers. It'll read
     *  messages, dispatch, send the response if any until
     *  done. This assumes one native server thread talking
     *  with a different client thread ( on the java side ).
     *  It does not work for jni or doors or other transports
     *  where a single thread is used for the whole processing.
     */
    int (*processCallbacks)(struct jk_env *env,
                            struct jk_workerEnv *_this,
                            struct jk_endpoint *e,
                            struct jk_ws_service *r );

    /**
     * Special init function to be called from the parent process
     * ( with root privs ? ).
     * Used to create the scoreboard ( workaround required at least for HPUX )
     *
     * init() will be called for each child, parentInit() only once.
     */
    int (*parentInit)(struct jk_env *env, struct jk_workerEnv *_this);

    /**
     *  Init the workers, prepare the worker environment. Will read
     *  all properties and set the jk acordignly.
     * 
     *  Replaces wc_open
     */
    int (*init)(struct jk_env *env, struct jk_workerEnv *_this);

    /** Close all workers, clean up
     *
     */
    void (*close)(struct jk_env *env, struct jk_workerEnv *_this);
};


typedef struct jk_workerEnv jk_workerEnv_t;


#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /* JK_WORKERENV_H */
