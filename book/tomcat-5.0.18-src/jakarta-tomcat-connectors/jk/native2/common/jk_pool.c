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
 * Description: Simple memory pool                                         *
 * Author:      Gal Shachor <shachor@il.ibm.com>                           *
 * Version:     $Revision: 1.17 $                                           *
 ***************************************************************************/

#error "jk_pool is deprecated"

#include "jk_pool.h"
#include "jk_env.h"

#define DEFAULT_DYNAMIC 10
/* #define JK_DEBUG_POOL 1 */

/* Private data for jk pools
 */
struct jk_pool_private {
    jk_pool_t *parent;    
    int size;      
    int pos;       
    int dyn_size;  
    int dyn_pos;   
    void     **dynamic;
    char  *buf;
};

typedef struct jk_pool_private jk_pool_private_t;

int jk2_pool_create( jk_env_t *env, jk_pool_t **newPool,
                    jk_pool_t *parent, int size );

int JK_METHOD jk2_pool_factory( jk_env_t *env, void **result,
                               char *type, char *name);

static jk_pool_t *jk2_pool_createChild(jk_env_t *env,
                                       jk_pool_t *parent, int size);

static void *jk2_pool_dyn_alloc(jk_env_t *env, jk_pool_t *p, 
                                size_t size);

static void jk2_pool_reset(jk_env_t *env, jk_pool_t *p);

static void jk2_pool_close(jk_env_t *env, jk_pool_t *p);

static void *jk2_pool_alloc(jk_env_t *env, jk_pool_t *p, size_t size);

static void *jk2_pool_calloc(jk_env_t *env, jk_pool_t *p, size_t size);

static void *jk2_pool_strdup(jk_env_t *env, jk_pool_t *p, const char *s);

static void *jk2_pool_strdup2ascii(jk_env_t *env, jk_pool_t *p, const char *s);

static void *jk2_pool_strdup2ebcdic(jk_env_t *env, jk_pool_t *p, const char *s);

static void *jk2_pool_realloc(jk_env_t *env, jk_pool_t *p, size_t sz,const void *old,
                              size_t old_sz);
static void *jk2_pool_strcat(jk_env_t *env, jk_pool_t *p, ...);

int jk2_pool_create(jk_env_t *env, jk_pool_t **newPool,
                    jk_pool_t *parent, int size)
{
    jk_pool_private_t *pp;
    
    jk_pool_t *_this=(jk_pool_t *)malloc( sizeof( jk_pool_t ));
    /*     jk_pool_t *_this=(jk_pool_t *)malloc( sizeof( jk_pool_t ) + */
    /*                                           sizeof( jk_pool_private_t ) +*/
    /*                                           size ); */
    pp=(jk_pool_private_t *)malloc( sizeof( jk_pool_private_t ));
    
    _this->_private=pp;

    /* XXX strange, but I assume the size is in bytes, not atom_t */
    pp->buf=(char *)malloc( size );

    pp->pos  = 0;
    pp->size = size;

    pp->dyn_pos = 0;
    pp->dynamic = NULL;
    pp->dyn_size = 0;
    pp->parent=parent;

    /* methods */
    _this->create=jk2_pool_createChild;
    _this->close=jk2_pool_close;
    _this->reset=jk2_pool_reset;
    
    _this->alloc=jk2_pool_alloc;
    _this->calloc=jk2_pool_calloc;
    _this->pstrdup=jk2_pool_strdup;
    _this->pstrdup2ascii=jk2_pool_strdup2ascii;
    _this->pstrdup2ebcdic=jk2_pool_strdup2ebcdic;
    _this->realloc=jk2_pool_realloc;
    _this->pstrcat=jk2_pool_strcat;

    *newPool = _this;
    
    return JK_OK;
}

static jk_pool_t *jk2_pool_createChild( jk_env_t *env, jk_pool_t *parent, int size ) {
    jk_pool_t *newPool;
    
    jk2_pool_create( env, &newPool, parent, size );
    return newPool;
}

static void jk2_pool_close(jk_env_t *env, jk_pool_t *p)
{
    jk_pool_private_t *pp;
    
    if(p==NULL || p->_private==NULL)
        return;

    pp=(jk_pool_private_t *)p->_private;
    
    jk2_pool_reset(env, p);
    if(pp->dynamic) {
        free(pp->dynamic);
    }
    if( pp->buf )
        free( pp->buf );
    free( pp );
    free( p );
}

static void jk2_pool_reset(jk_env_t *env, jk_pool_t *p)
{
    jk_pool_private_t *pp;
    
    if(p==NULL || p->_private ==NULL )
        return;

    pp=(jk_pool_private_t *)p->_private;

    if( pp->dyn_pos && pp->dynamic) {
        int i;
        for(i = 0 ; i < pp->dyn_pos ; i++) {
            if(pp->dynamic[i]) {
                free(pp->dynamic[i]);
            }
        }
    }

    pp->dyn_pos  = 0;
    pp->pos      = 0;
}

static void *jk2_pool_calloc(jk_env_t *env, jk_pool_t *p, 
                            size_t size)
{
    void *rc=jk2_pool_alloc( env, p, size );
    if( rc==NULL )
        return NULL;
    memset( rc, 0, size );
    return rc;
}

static void *jk2_pool_alloc(jk_env_t *env, jk_pool_t *p, 
                           size_t size)
{
    void *rc = NULL;
    jk_pool_private_t *pp;

    if(p==NULL || size <= 0)
        return NULL;

    pp=(jk_pool_private_t *)p->_private;

    /* Round size to the upper mult of 8 (or 16 on iSeries) */
    size--;
#ifdef AS400
        size /= 16;
        size = (size + 1) * 16;
#else
        size /= 8;
        size = (size + 1) * 8;
#endif

    if((pp->size - pp->pos) >= (int)size) {
        /* We have space */
        rc = &(pp->buf[pp->pos]);
        pp->pos += size;
    } else {
#ifdef JK_DEBUG_POOL
        printf("Dynamic alloc %d\n", pp->size );
#endif
        rc = jk2_pool_dyn_alloc(env, p, size);
    }

    return rc;
}

static void *jk2_pool_realloc(jk_env_t *env, jk_pool_t *p, size_t sz,
                             const void *old, size_t old_sz)
{
    void *rc;

    if(!p || (!old && old_sz)) {
        return NULL;
    }
    rc = jk2_pool_alloc(env, p, sz);
    if(rc==NULL)
        return NULL;

    memcpy(rc, old, old_sz);
    return rc;
}

static void *jk2_pool_a_strdup(jk_env_t *env, jk_pool_t *p, const char *s, int convmode)
{
    char *rc = NULL;
    if(s && p) {
        size_t size = strlen(s);
    
        if(!size)  {
            return "";
        }

        rc = jk2_pool_alloc(env, p, size+1);
        if(rc) {
            memcpy(rc, s, size);
        }
        rc[size]='\0';
        
        if (convmode == 1)
            jk_xlate_to_ascii(rc, size);
        else if (convmode == 2)
            jk_xlate_from_ascii(rc, size);        
    }

    return rc;
}

static void *jk2_pool_strdup(jk_env_t *env, jk_pool_t *p, const char *s)
{
    return (jk2_pool_a_strdup(env, p, s, 0));
}

static void *jk2_pool_strdup2ascii(jk_env_t *env, jk_pool_t *p, const char *s)
{
    return (jk2_pool_a_strdup(env, p, s, 1));
}

static void *jk2_pool_strdup2ebcdic(jk_env_t *env, jk_pool_t *p, const char *s)
{
    return (jk2_pool_a_strdup(env, p, s, 2));
}

/*
  static void jk2_dump_pool(jk_pool_t *p, 
                            FILE *f)
{
    fprintf(f, "Dumping for pool [%#lx]\n", p);
    fprintf(f, "size             [%d]\n", p->size);
    fprintf(f, "pos              [%d]\n", p->pos);
    fprintf(f, "buf              [%#lx]\n", p->buf);  
    fprintf(f, "dyn_size         [%d]\n", p->dyn_size);
    fprintf(f, "dyn_pos          [%d]\n", p->dyn_pos);
    fprintf(f, "dynamic          [%#lx]\n", p->dynamic);

    fflush(f);
}
*/

static void *jk2_pool_dyn_alloc(jk_env_t *env, jk_pool_t *p, size_t size)

{
    void *rc = NULL;
    jk_pool_private_t *pp;
    pp=(jk_pool_private_t *)p->_private;

    if(pp->dyn_size == pp->dyn_pos) {
        int new_dyn_size = pp->dyn_size + DEFAULT_DYNAMIC;
        void **new_dynamic = (void **)malloc(new_dyn_size * sizeof(void *));
        if(new_dynamic==NULL) {
            return NULL;
        }

        if(pp->dynamic) {
            memcpy(new_dynamic, 
                   pp->dynamic, 
                   pp->dyn_size * sizeof(void *));
            
            free(pp->dynamic);
        }

        pp->dynamic = new_dynamic;
        pp->dyn_size = new_dyn_size;
    } 

    rc = pp->dynamic[pp->dyn_pos] = malloc(size);
    if(pp->dynamic[pp->dyn_pos]) {
        pp->dyn_pos ++;
    }
    return rc;
}

/** this is used to cache lengths in pstrcat */
#define MAX_SAVED_LENGTHS  6
 
static void *jk2_pool_strcat(jk_env_t *env, jk_pool_t *p, ...)
{

    char *cp, *argp, *res;
    size_t saved_lengths[MAX_SAVED_LENGTHS];
    int nargs = 0;

    /* Pass one --- find length of required string */

    size_t len = 0;
    va_list adummy;

    va_start(adummy, p);

    while ((cp = va_arg(adummy, char *)) != NULL) {
        size_t cplen = strlen(cp);
        if (nargs < MAX_SAVED_LENGTHS) {
            saved_lengths[nargs++] = cplen;
        }
        len += cplen;
    }

    va_end(adummy);

    /* Allocate the required string */

    res = (char *) jk2_pool_alloc(env, p, len+1);
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



/* Not used yet */
int JK_METHOD jk2_pool_factory( jk_env_t *env, void **result,
                               char *type, char *name)
{
    jk_pool_t *_this=(jk_pool_t *)calloc( 1, sizeof(jk_pool_t));

    *result=_this;
    
    return JK_OK;
}


