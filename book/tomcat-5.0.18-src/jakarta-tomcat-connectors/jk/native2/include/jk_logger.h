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
 * Description: Logger object definitions                                  *
 * Author:      Gal Shachor <shachor@il.ibm.com>                           *
 * Version:     $Revision: 1.9 $                                           *
 ***************************************************************************/

#ifndef JK_LOGGER_H
#define JK_LOGGER_H

#include "jk_env.h"
#include "jk_global.h"
#include "jk_map.h"

#ifdef __cplusplus
extern "C" {
#endif /* __cplusplus */

struct jk_map;
struct jk_env;
struct jk_logger;
typedef struct jk_logger jk_logger_t;

/* Logger object.
 *  XXX level should be moved per component ( to control the generation of messages ),
 *  the level param in the param should be used only as information ( to be displayed
 *  in the log ).
 */
struct jk_logger {
    struct jk_bean *mbean;
    char *name;
    void *logger_private;
    int  level;

    int (JK_METHOD *init)( struct jk_env *env, jk_logger_t *_this );

    void (JK_METHOD *close)( struct jk_env *env, jk_logger_t *_this );

    int (JK_METHOD *log)(struct jk_env *env,
                         jk_logger_t *_this,
                         int level,
                         const char *what);

    int (JK_METHOD *jkLog)(struct jk_env *env,
                           jk_logger_t *_this,
                           const char *file,
                           int line,
                           int level,
                           const char *fmt, ...);

    int (JK_METHOD *jkVLog)(struct jk_env *env,
                            jk_logger_t *_this,
                            const char *file,
                            int line,
                            int level,
                            const char *fmt,
                            va_list msg);

};

#define JK_LOG_DEBUG_LEVEL 0
#define JK_LOG_INFO_LEVEL  1
#define JK_LOG_ERROR_LEVEL 2
#define JK_LOG_EMERG_LEVEL 3

#define JK_LOG_DEBUG_VERB   "debug"
#define JK_LOG_INFO_VERB    "info"
#define JK_LOG_ERROR_VERB   "error"
#define JK_LOG_EMERG_VERB   "emerg"

#define JK_LOG_DEBUG __FILE__,__LINE__,JK_LOG_DEBUG_LEVEL
#define JK_LOG_INFO  __FILE__,__LINE__,JK_LOG_INFO_LEVEL
#define JK_LOG_ERROR __FILE__,__LINE__,JK_LOG_ERROR_LEVEL
#define JK_LOG_EMERG __FILE__,__LINE__,JK_LOG_EMERG_LEVEL

int jk2_logger_file_parseLogLevel(struct jk_env *env, const char *level);

#ifdef __cplusplus
}
#endif /* __cplusplus */

#endif /* JK_LOGGER_H */
