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
 * Pool using APR.
 *
 * @author Costin Manolache
 */

#include "apr_pools.h"
#include "apr_strings.h"
#include "apr_network_io.h"
#include "apr_errno.h"
#include "apr_general.h"

#include "jk_global.h"
#include "jk_pool.h"
#include "jk_env.h"

/* 
   JK_APR_POOL_DEBUG will enable verbose messages on allocation.

   What's important is to see 'reset' after each request.
*/


/** Nothing - apache will take care ??
 */
static void jk2_pool_apr_close(jk_env_t *env, jk_pool_t *p)
{
#ifdef JK_APR_POOL_DEBUG
    fprintf(stderr, "apr_close %#lx\n", p);
#endif
}

/** Nothing - apache will take care.
    XXX with jk pools we can implement 'recycling',
    not sure what's the equivalent for apache
*/
static void jk2_pool_apr_reset(jk_env_t *env, jk_pool_t *p)
{
#ifdef JK_APR_POOL_DEBUG
    fprintf(stderr, "apr_reset %#lx\n", p);
#endif
    apr_pool_clear(p->_private);
}

static void *jk2_pool_apr_calloc(jk_env_t *env, jk_pool_t *p, 
                                 size_t size)
{
#ifdef JK_APR_POOL_DEBUG
    fprintf(stderr, "apr_calloc %#lx %d\n", p, size);
#endif
    /* assert( p->_private != NULL ) */
    return apr_pcalloc( (apr_pool_t *)p->_private, (apr_size_t)size);
}

static void *jk2_pool_apr_alloc(jk_env_t *env, jk_pool_t *p, 
                                size_t size)
{
#ifdef JK_APR_POOL_DEBUG
    fprintf(stderr, "apr_alloc %#lx %d\n", p, size);
#endif

    return apr_palloc( (apr_pool_t *)p->_private, (apr_size_t)size);
}

static void *jk2_pool_apr_realloc(jk_env_t *env, jk_pool_t *p, 
                                  size_t sz,
                                  const void *old,
                                  size_t old_sz)
{
    void *rc;

#ifdef JK_APR_POOL_DEBUG
    fprintf(stderr, "apr_realloc %#lx %d\n", p, sz);
#endif
    if(!p || (!old && old_sz)) {
        return NULL;
    }

    rc = jk2_pool_apr_alloc(env, p, sz);
    if(rc) {
        memcpy(rc, old, old_sz);
    }

    return rc;
}

static void *jk2_pool_apr_strdup(jk_env_t *env, jk_pool_t *p, 
                                 const char *s)
{
#ifdef JK_APR_POOL_DEBUG
    fprintf(stderr, "apr_strdup %#lx %d\n", p, ((s==NULL)?-1: (int)strlen(s)));
#endif
    return apr_pstrdup( (apr_pool_t *)p->_private, s);
}

static void jk2_pool_apr_initMethods(jk_env_t *env,  jk_pool_t *_this );

static jk_pool_t *jk2_pool_apr_createChild( jk_env_t *env, jk_pool_t *_this,
                                           int sizeHint ) {
    apr_pool_t *parentAprPool=_this->_private;
    apr_pool_t *childAprPool;
    jk_pool_t *newPool;

    apr_pool_create( &childAprPool, parentAprPool );

    newPool=(jk_pool_t *)apr_palloc(parentAprPool, sizeof( jk_pool_t ));
    
    jk2_pool_apr_initMethods( env, newPool );
    newPool->_private=childAprPool;
    
    return newPool;
}

/** this is used to cache lengths in pstrcat */
#define MAX_SAVED_LENGTHS  6
 
static void *jk2_pool_apr_strcat(jk_env_t *env, jk_pool_t *p, ...)
{

    char *cp, *argp, *res;
    apr_size_t saved_lengths[MAX_SAVED_LENGTHS];
    int nargs = 0;

    /* Pass one --- find length of required string */

    apr_size_t len = 0;
    va_list adummy;

    va_start(adummy, p);

    while ((cp = va_arg(adummy, char *)) != NULL) {
        apr_size_t cplen = strlen(cp);
        if (nargs < MAX_SAVED_LENGTHS) {
            saved_lengths[nargs++] = cplen;
        }
        len += cplen;
    }

    va_end(adummy);

    /* Allocate the required string */

    res = (char *) apr_palloc(p->_private, len+1);
    cp = res;

    /* Pass two --- copy the argument strings into the result space */

    va_start(adummy, p);

    nargs = 0;
    while ((argp = va_arg(adummy, char *)) != NULL) {
        if (nargs < MAX_SAVED_LENGTHS) {
            len = saved_lengths[nargs++];
        }
        else {
            len = strlen(argp);
        }
 
        memcpy(cp, argp, len);
        cp += len;
    }

    va_end(adummy);

    /* Return the result string */

    *cp = '\0';

    return res; 
}


int JK_METHOD jk2_pool_apr_create( jk_env_t *env, jk_pool_t **newPool, jk_pool_t *parent,
                                   void *aprPoolV)
{
    jk_pool_t *_this;
    apr_pool_t *aprPool=(apr_pool_t *)aprPoolV;

    _this=(jk_pool_t *)apr_palloc(aprPool, sizeof( jk_pool_t ));
    *newPool = _this;

    _this->_private=aprPool;
    jk2_pool_apr_initMethods( env, _this );
    return JK_OK;
}

static void jk2_pool_apr_initMethods(jk_env_t *env,  jk_pool_t *_this )
{
    /* methods */
    _this->create=jk2_pool_apr_createChild;
    _this->close=jk2_pool_apr_close;
    _this->reset=jk2_pool_apr_reset;
    _this->alloc=jk2_pool_apr_alloc;
    _this->calloc=jk2_pool_apr_calloc;
    _this->pstrdup=jk2_pool_apr_strdup;
    _this->realloc=jk2_pool_apr_realloc;
    _this->pstrcat=jk2_pool_apr_strcat;
}


/* Not used yet */
int  jk2_pool_apr_factory(jk_env_t *env, jk_pool_t *pool,
                          jk_bean_t *result,
                          char *type, char *name)
{
    jk_pool_t *_this=(jk_pool_t *)calloc( 1, sizeof(jk_pool_t));

    result->object=_this;
    
    return JK_OK;
}
