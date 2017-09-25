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
 * Implementation of map using apr_table. This avoids copying the headers,
 * env, etc in jk_service - we can just wrap them.
 *
 * Note that this _require_ that apr pools are used ( can't be used
 * with jk_pools ), i.e. you must use apr for both pools and maps.
 *
 * @author Costin Manolache
 */

#include "jk_pool.h"
#include "jk_env.h"
#include "apr_pools.h"
#include "apr_strings.h"
#include "apr_tables.h"

#include "jk_apache2.h"


static void *jk2_map_aprtable_get( struct jk_env *env, struct jk_map *_this,
                                  const char *name)
{
    apr_table_t *aprMap=_this->_private;
    return (void *)apr_table_get( aprMap, name );
}

static int jk2_map_aprtable_put( struct jk_env *env, struct jk_map *_this,
                                const char *name, void *value,
                                void **oldValue )
{
    apr_table_t *aprMap=_this->_private;
    if( oldValue != NULL ) {
        *oldValue=(void *)apr_table_get( aprMap, (char *)name );
    }
    
    apr_table_setn( aprMap, name, (char *)value );
    
    return JK_OK;
}

static int jk2_map_aprtable_add( struct jk_env *env, struct jk_map *_this,
                                const char *name, void *value )
{
    apr_table_t *aprMap=_this->_private;
    
    apr_table_addn( aprMap, name, (char *)value );
    
    return JK_OK;
}

static int jk2_map_aprtable_size( struct jk_env *env, struct jk_map *_this )
{
    apr_table_t *aprMap=_this->_private;
    const apr_array_header_t *ah = apr_table_elts( aprMap );

    return ah->nelts;

}

static char * jk2_map_aprtable_nameAt( struct jk_env *env, struct jk_map *_this,
                                      int pos)
{
    apr_table_t *aprMap=_this->_private;
    const apr_array_header_t *ah = apr_table_elts( aprMap );
    apr_table_entry_t *elts = (apr_table_entry_t *)ah->elts;
               

    return elts[pos].key;
}

static void * jk2_map_aprtable_valueAt( struct jk_env *env, struct jk_map *_this,
                                       int pos)
{
    apr_table_t *aprMap=_this->_private;
    const apr_array_header_t *ah = apr_table_elts( aprMap );
    apr_table_entry_t *elts = (apr_table_entry_t *)ah->elts;

    return elts[pos].val;
}

static void jk2_map_aprtable_init(jk_env_t *env, jk_map_t *m, int initialSize,
                                void *wrappedObj)
{
    m->_private=wrappedObj;
}

static void jk2_map_aprtable_clear(jk_env_t *env, jk_map_t *m)
{

}


/* Not used yet */
int JK_METHOD jk2_map_aprtable_factory(jk_env_t *env, jk_pool_t *pool,
                                       jk_bean_t *result,
                                       const char *type, const char *name)
{
    jk_map_t *_this=(jk_map_t *)pool->calloc( env, pool, sizeof(jk_map_t));

    result->object=_this;
    
    _this->get=jk2_map_aprtable_get;
    _this->put=jk2_map_aprtable_put;
    _this->add=jk2_map_aprtable_add;
    _this->size=jk2_map_aprtable_size;
    _this->nameAt=jk2_map_aprtable_nameAt;
    _this->valueAt=jk2_map_aprtable_valueAt;
    _this->init=jk2_map_aprtable_init;
    _this->clear=jk2_map_aprtable_clear;
    
    return JK_OK;
}


