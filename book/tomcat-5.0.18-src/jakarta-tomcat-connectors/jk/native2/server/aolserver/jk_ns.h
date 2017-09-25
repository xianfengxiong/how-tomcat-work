/* ========================================================================= *
 *                                                                           *
 *                 The Apache Software License,  Version 1.1                 *
 *                                                                           *
 *          Copyright (c) 1999-2002 The Apache Software Foundation.          *
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
 * Description: AOLserver plugin for Jakarta/Tomcat                         
 * Author:      Alexander Leykekh, aolserver@aol.net 
 * Version:     $Revision: 1.2 $                                           
 */

#ifndef JK_NS_H
#define JK_NS_H

#include "apu_compat.h"
#include "apr_lib.h"
#include "apr_date.h"
#include "apr_strings.h"
#include "apr_pools.h"
#include "apr_tables.h"
#include "apr_hash.h"

#include "ns.h"

#include "jk_global.h"
#include "jk_map.h"
#include "jk_pool.h"
#include "jk_env.h"
#include "jk_service.h"
#include "jk_worker.h"
#include "jk_workerEnv.h"
#include "jk_uriMap.h"
#include "jk_requtil.h"

extern int Ns_ModuleVersion;

int Ns_ModuleInit(char *server, char *module);

int JK_METHOD jk2_service_ns_init(jk_env_t *env, jk_ws_service_t *s, char* serverNameOverride);

int JK_METHOD jk2_logger_ns_factory(jk_env_t *env, jk_pool_t *pool,
                                         jk_bean_t *result, const char *type, const char *name);

/* from ../../common/jk_pool_apr.c */
int JK_METHOD jk2_pool_apr_factory(jk_env_t *env, jk_pool_t *pool,
                                   jk_bean_t *result, const char *type, const char *name);

int JK_METHOD jk2_map_aprtable_factory(jk_env_t *env, jk_pool_t *pool,
                                       jk_bean_t *result,
                                       const char *type, const char *name);

#endif /* JK_NS_H */
