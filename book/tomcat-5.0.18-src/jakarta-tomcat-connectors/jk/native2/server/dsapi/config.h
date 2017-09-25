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

#ifndef __config_h
#define __config_h

#define MAKEVERSION(a, b, c, d) \
	(((a) << 24) + ((b) << 16) + ((c) << 8) + (d))

#define NONBLANK(s) \
	(NULL != (s) && '\0' != *(s))

/* the _memicmp() function is available */
#if defined(WIN32)

#define HAVE_MEMICMP
#define PATHSEP '\\'

#elif defined(LINUX)

#undef HAVE_MEMICMP
#define PATHSEP '/'

#elif defined(SOLARIS)

#undef HAVE_MEMICMP
#define PATHSEP '/'

#else
#error Please define one of WIN32, LINUX or SOLARIS
#endif

/* define if you don't have the Notes C API which is available from
 *
 *    http://www.lotus.com/rw/dlcapi.nsf
 */
/* #undef NO_CAPI */

#ifdef _DEBUG
#define DEBUG(args) \
	do { _printf args ; } while (0)
#else
#define DEBUG(args) \
	do { } while (0)
#endif

#if !defined(DLLEXPORT)
#if defined(WIN32) && !defined(TESTING)
#define DLLEXPORT __declspec(dllexport)
#else
#define DLLEXPORT
#endif
#endif

/* Configuration tags */
#define SERVER_ROOT_TAG		"serverRoot"
#define WORKER_FILE_TAG		"workersFile"
#define TOMCAT_START_TAG	"tomcatStart"
#define TOMCAT_STOP_TAG		"tomcatStop"
#define TOMCAT_TIMEOUT_TAG	"tomcatTimeout"
#define VERSION				"2.0.0"
#define VERSION_STRING		"Jakarta/DSAPI/" VERSION
#define FILTERDESC			"Apache Tomcat Interceptor (" VERSION_STRING ")"
#define SERVERDFLT			"Lotus Domino"
#define REGISTRY_LOCATION	"Software\\Apache Software Foundation\\Jakarta Dsapi Redirector\\2.0"
#define TOMCAT_STARTSTOP_TO	30000				/* 30 seconds */
#define CONTENT_LENGTH		"Content-length"	/* Name of CL header */
#define PROPERTIES_EXT		".properties"

#endif /* __config_h */
