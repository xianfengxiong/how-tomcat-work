/*
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 1999-2001 The Apache Software Foundation.  All rights
 * reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 *
 * 1. Redistributions of source code must retain the above copyright
 *    notice, this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright
 *    notice, this list of conditions and the following disclaimer in
 *    the documentation and/or other materials provided with the
 *    distribution.
 *
 * 3. The end-user documentation included with the redistribution, if
 *    any, must include the following acknowlegement:
 *       "This product includes software developed by the
 *        Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowlegement may appear in the software itself,
 *    if and wherever such third-party acknowlegements normally appear.
 *
 * 4. The names "The Jakarta Project", "Tomcat", and "Apache Software
 *    Foundation" must not be used to endorse or promote products derived
 *    from this software without prior written permission. For written
 *    permission, please contact apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache"
 *    nor may "Apache" appear in their names without prior written
 *    permission of the Apache Group.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED
 * WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES
 * OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED.  IN NO EVENT SHALL THE APACHE SOFTWARE FOUNDATION OR
 * ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF
 * USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT
 * OF THE USE OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF
 * SUCH DAMAGE.
 * ====================================================================
 *
 * This software consists of voluntary contributions made by many
 * individuals on behalf of the Apache Software Foundation.  For more
 * information on the Apache Software Foundation, please see
 * <http://www.apache.org/>.
 *
 * [Additional notices, if required by prior licensing conditions]
 *
 */


package org.apache.catalina.valves;


import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.TimeZone;

import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.catalina.HttpResponse;
import org.apache.catalina.Lifecycle;
import org.apache.catalina.LifecycleException;
import org.apache.catalina.LifecycleListener;
import org.apache.catalina.Request;
import org.apache.catalina.Response;
import org.apache.catalina.ValveContext;
import org.apache.catalina.util.LifecycleSupport;
import org.apache.catalina.util.StringManager;


/**
 * <p>Implementation of the <b>Valve</b> interface that generates a web server
 * access log with the detailed line contents matching a configurable pattern.
 * The syntax of the available patterns is similar to that supported by the
 * Apache <code>mod_log_config</code> module.  As an additional feature,
 * automatic rollover of log files when the date changes is also supported.</p>
 *
 * <p>Patterns for the logged message may include constant text or any of the
 * following replacement strings, for which the corresponding information
 * from the specified Response is substituted:</p>
 * <ul>
 * <li><b>%a</b> - Remote IP address
 * <li><b>%A</b> - Local IP address
 * <li><b>%b</b> - Bytes sent, excluding HTTP headers, or '-' if no bytes
 *     were sent
 * <li><b>%B</b> - Bytes sent, excluding HTTP headers
 * <li><b>%h</b> - Remote host name
 * <li><b>%H</b> - Request protocol
 * <li><b>%l</b> - Remote logical username from identd (always returns '-')
 * <li><b>%m</b> - Request method
 * <li><b>%p</b> - Local port
 * <li><b>%q</b> - Query string (prepended with a '?' if it exists, otherwise
 *     an empty string
 * <li><b>%r</b> - First line of the request
 * <li><b>%s</b> - HTTP status code of the response
 * <li><b>%S</b> - User session ID
 * <li><b>%t</b> - Date and time, in Common Log Format format
 * <li><b>%u</b> - Remote user that was authenticated
 * <li><b>%U</b> - Requested URL path
 * <li><b>%v</b> - Local server name
 * <li><b>%D</b> - Time taken to process the request, in millis
 * <li><b>%T</b> - Time taken to process the request, in seconds
 * </ul>
 * <p>In addition, the caller can specify one of the following aliases for
 * commonly utilized patterns:</p>
 * <ul>
 * <li><b>common</b> - <code>%h %l %u %t "%r" %s %b</code>
 * <li><b>combined</b> -
 *   <code>%h %l %u %t "%r" %s %b "%{Referer}i" "%{User-Agent}i"</code>
 * </ul>
 *
 * <p>
 * There is also support to write information from the cookie, incoming
 * header, the Session or something else in the ServletRequest.<br>
 * It is modeled after the apache syntax:
 * <ul>
 * <li><code>%{xxx}i</code> for incoming headers
 * <li><code>%{xxx}c</code> for a specific cookie
 * <li><code>%{xxx}r</code> xxx is an attribute in the ServletRequest
 * <li><code>%{xxx}s</code> xxx is an attribute in the HttpSession
 * </ul>
 * </p>
 *
 * <p>
 * Conditional logging is also supported. This can be done with the
 * <code>condition</code> property.
 * If the value returned from ServletRequest.getAttribute(condition)
 * yields a non-null value. The logging will be skipped.
 * </p>
 *
 * @author Craig R. McClanahan
 * @author Jason Brittain
 * @version $Revision: 1.8 $ $Date: 2004/01/15 16:18:11 $
 */

public final class AccessLogValve
    extends ValveBase
    implements Lifecycle {


    // ----------------------------------------------------------- Constructors


    /**
     * Construct a new instance of this class with default property values.
     */
    public AccessLogValve() {

        super();
        setPattern("common");


    }


    // ----------------------------------------------------- Instance Variables


    /**
     * The as-of date for the currently open log file, or a zero-length
     * string if there is no open log file.
     */
    private String dateStamp = "";


    /**
     * The directory in which log files are created.
     */
    private String directory = "logs";


    /**
     * The descriptive information about this implementation.
     */
    protected static final String info =
        "org.apache.catalina.valves.AccessLogValve/1.0";


    /**
     * The lifecycle event support for this component.
     */
    protected LifecycleSupport lifecycle = new LifecycleSupport(this);


    /**
     * The set of month abbreviations for log messages.
     */
    protected static final String months[] =
    { "Jan", "Feb", "Mar", "Apr", "May", "Jun",
      "Jul", "Aug", "Sep", "Oct", "Nov", "Dec" };


    /**
     * If the current log pattern is the same as the common access log
     * format pattern, then we'll set this variable to true and log in
     * a more optimal and hard-coded way.
     */
    private boolean common = false;


    /**
     * For the combined format (common, plus useragent and referer), we do
     * the same
     */
    private boolean combined = false;


    /**
     * The pattern used to format our access log lines.
     */
    private String pattern = null;


    /**
     * The prefix that is added to log file filenames.
     */
    private String prefix = "access_log.";


    /**
     * Should we rotate our log file? Default is true (like old behavior)
     */
    private boolean rotatable = true;


    /**
     * The string manager for this package.
     */
    private StringManager sm =
        StringManager.getManager(Constants.Package);


    /**
     * Has this component been started yet?
     */
    private boolean started = false;


    /**
     * The suffix that is added to log file filenames.
     */
    private String suffix = "";


    /**
     * The PrintWriter to which we are currently logging, if any.
     */
    private PrintWriter writer = null;


    /**
     * A date formatter to format a Date into a date in the format
     * "yyyy-MM-dd".
     */
    private SimpleDateFormat dateFormatter = null;


    /**
     * A date formatter to format Dates into a day string in the format
     * "dd".
     */
    private SimpleDateFormat dayFormatter = null;


    /**
     * A date formatter to format a Date into a month string in the format
     * "MM".
     */
    private SimpleDateFormat monthFormatter = null;


    /**
     * Time taken formatter for 3 decimal places.
     */
     private DecimalFormat timeTakenFormatter = null;


    /**
     * A date formatter to format a Date into a year string in the format
     * "yyyy".
     */
    private SimpleDateFormat yearFormatter = null;


    /**
     * A date formatter to format a Date into a time in the format
     * "kk:mm:ss" (kk is a 24-hour representation of the hour).
     */
    private SimpleDateFormat timeFormatter = null;


    /**
     * The time zone relative to GMT.
     */
    private String timeZone = null;


    /**
     * The system time when we last updated the Date that this valve
     * uses for log lines.
     */
    private Date currentDate = null;


    /**
     * When formatting log lines, we often use strings like this one (" ").
     */
    private String space = " ";


    /**
     * Resolve hosts.
     */
    private boolean resolveHosts = false;


    /**
     * Instant when the log daily rotation was last checked.
     */
    private long rotationLastChecked = 0L;


    /**
     * Are we doing conditional logging. default false.
     */
    private String condition = null;


    /**
     * Date format to place in log file name. Use at your own risk!
     */
    private String fileDateFormat = null;

    // ------------------------------------------------------------- Properties


    /**
     * Return the directory in which we create log files.
     */
    public String getDirectory() {

        return (directory);

    }


    /**
     * Set the directory in which we create log files.
     *
     * @param directory The new log file directory
     */
    public void setDirectory(String directory) {

        this.directory = directory;

    }


    /**
     * Return descriptive information about this implementation.
     */
    public String getInfo() {

        return (this.info);

    }


    /**
     * Return the format pattern.
     */
    public String getPattern() {

        return (this.pattern);

    }


    /**
     * Set the format pattern, first translating any recognized alias.
     *
     * @param pattern The new pattern
     */
    public void setPattern(String pattern) {

        if (pattern == null)
            pattern = "";
        if (pattern.equals(Constants.AccessLog.COMMON_ALIAS))
            pattern = Constants.AccessLog.COMMON_PATTERN;
        if (pattern.equals(Constants.AccessLog.COMBINED_ALIAS))
            pattern = Constants.AccessLog.COMBINED_PATTERN;
        this.pattern = pattern;

        if (this.pattern.equals(Constants.AccessLog.COMMON_PATTERN))
            common = true;
        else
            common = false;

        if (this.pattern.equals(Constants.AccessLog.COMBINED_PATTERN))
            combined = true;
        else
            combined = false;

    }


    /**
     * Return the log file prefix.
     */
    public String getPrefix() {

        return (prefix);

    }


    /**
     * Set the log file prefix.
     *
     * @param prefix The new log file prefix
     */
    public void setPrefix(String prefix) {

        this.prefix = prefix;

    }


    /**
     * Should we rotate the logs
     */
    public boolean isRotatable() {

        return rotatable;

    }


    /**
     * Set the value is we should we rotate the logs
     *
     * @param rotatable true is we should rotate.
     */
    public void setRotatable(boolean rotatable) {

        this.rotatable = rotatable;

    }


    /**
     * Return the log file suffix.
     */
    public String getSuffix() {

        return (suffix);

    }


    /**
     * Set the log file suffix.
     *
     * @param suffix The new log file suffix
     */
    public void setSuffix(String suffix) {

        this.suffix = suffix;

    }


    /**
     * Set the resolve hosts flag.
     *
     * @param resolveHosts The new resolve hosts value
     */
    public void setResolveHosts(boolean resolveHosts) {

        this.resolveHosts = resolveHosts;

    }


    /**
     * Get the value of the resolve hosts flag.
     */
    public boolean isResolveHosts() {

        return resolveHosts;

    }


    /**
     * Return whether the attribute name to look for when
     * performing conditional loggging. If null, every
     * request is logged.
     */
    public String getCondition() {

        return condition;

    }


    /**
     * Set the ServletRequest.attribute to look for to perform
     * conditional logging. Set to null to log everything.
     *
     * @param condition Set to null to log everything
     */
    public void setCondition(String condition) {

        this.condition = condition;

    }

    /**
     *  Return the date format date based log rotation.
     */
    public String getFileDateFormat() {
        return fileDateFormat;
    }


    /**
     *  Set the date format date based log rotation.
     */
    public void setFileDateFormat(String fileDateFormat) {
        this.fileDateFormat =  fileDateFormat;
    }

    // --------------------------------------------------------- Public Methods


    /**
     * Log a message summarizing the specified request and response, according
     * to the format specified by the <code>pattern</code> property.
     *
     * @param request Request being processed
     * @param response Response being processed
     * @param context The valve context used to invoke the next valve
     *  in the current processing pipeline
     *
     * @exception IOException if an input/output error has occurred
     * @exception ServletException if a servlet error has occurred
     */
    public void invoke(Request request, Response response,
                       ValveContext context)
        throws IOException, ServletException {

        // Pass this request on to the next valve in our pipeline
        long t1=System.currentTimeMillis();

        context.invokeNext(request, response);

        long t2=System.currentTimeMillis();
        long time=t2-t1;

        if (condition!=null &&
                null!=request.getRequest().getAttribute(condition)) {
            return;
        }


        Date date = getDate();
        StringBuffer result = new StringBuffer();

        // Check to see if we should log using the "common" access log pattern
        if (common || combined) {
            String value = null;

            ServletRequest req = request.getRequest();
            HttpServletRequest hreq = (HttpServletRequest) req;

            if (isResolveHosts())
                result.append(req.getRemoteHost());
            else
                result.append(req.getRemoteAddr());

            result.append(" - ");

            value = hreq.getRemoteUser();
            if (value == null)
                result.append("- ");
            else {
                result.append(value);
                result.append(space);
            }

            result.append("[");
            result.append(dayFormatter.format(date));            // Day
            result.append('/');
            result.append(lookup(monthFormatter.format(date))); // Month
            result.append('/');
            result.append(yearFormatter.format(date));            // Year
            result.append(':');
            result.append(timeFormatter.format(date));        // Time
            result.append(space);
            result.append(timeZone);                            // Time Zone
            result.append("] \"");

            result.append(hreq.getMethod());
            result.append(space);
            result.append(hreq.getRequestURI());
            if (hreq.getQueryString() != null) {
                result.append('?');
                result.append(hreq.getQueryString());
            }
            result.append(space);
            result.append(hreq.getProtocol());
            result.append("\" ");

            result.append(((HttpResponse) response).getStatus());

            result.append(space);

            int length = response.getContentCount();

            if (length <= 0)
                value = "-";
            else
                value = "" + length;
            result.append(value);

            if (combined) {
                result.append(space);
                result.append("\"");
                String referer = hreq.getHeader("referer");
                if(referer != null)
                    result.append(referer);
                else
                    result.append("-");
                result.append("\"");

                result.append(space);
                result.append("\"");
                String ua = hreq.getHeader("user-agent");
                if(ua != null)
                    result.append(ua);
                else
                    result.append("-");
                result.append("\"");
            }

        } else {
            // Generate a message based on the defined pattern
            boolean replace = false;
            for (int i = 0; i < pattern.length(); i++) {
                char ch = pattern.charAt(i);
                if (replace) {
                    /* For code that processes {, the behavior will be ... if I
                     * do not enounter a closing } - then I ignore the {
                     */
                    if ('{' == ch){
                        StringBuffer name = new StringBuffer();
                        int j = i + 1;
                        for(;j < pattern.length() && '}' != pattern.charAt(j); j++) {
                            name.append(pattern.charAt(j));
                        }
                        if (j+1 < pattern.length()) {
                            /* the +1 was to account for } which we increment now */
                            j++;
                            result.append(replace(name.toString(),
                                                pattern.charAt(j),
                                                request,
                                                response));
                            i=j; /*Since we walked more than one character*/
                        } else {
                            //D'oh - end of string - pretend we never did this
                            //and do processing the "old way"
                            result.append(replace(ch, date, request, response, time));
                        }
                    } else {
                        result.append(replace(ch, date, request, response,time ));
                    }
                    replace = false;
                } else if (ch == '%') {
                    replace = true;
                } else {
                    result.append(ch);
                }
            }
        }
        log(result.toString(), date);

    }


    // -------------------------------------------------------- Private Methods


    /**
     * Close the currently open log file (if any)
     */
    private synchronized void close() {

        if (writer == null)
            return;
        writer.flush();
        writer.close();
        writer = null;
        dateStamp = "";

    }


    /**
     * Log the specified message to the log file, switching files if the date
     * has changed since the previous log call.
     *
     * @param message Message to be logged
     * @param date the current Date object (so this method doesn't need to
     *        create a new one)
     */
    public void log(String message, Date date) {

        if (rotatable){
            // Only do a logfile switch check once a second, max.
            long systime = System.currentTimeMillis();
            if ((systime - rotationLastChecked) > 1000) {

                // We need a new currentDate
                currentDate = new Date(systime);
                rotationLastChecked = systime;

                // Check for a change of date
                String tsDate = dateFormatter.format(currentDate);

                // If the date has changed, switch log files
                if (!dateStamp.equals(tsDate)) {
                    synchronized (this) {
                        if (!dateStamp.equals(tsDate)) {
                            close();
                            dateStamp = tsDate;
                            open();
                        }
                    }
                }

            }
        }

        // Log this message
        if (writer != null) {
            writer.println(message);
        }

    }


    /**
     * Return the month abbreviation for the specified month, which must
     * be a two-digit String.
     *
     * @param month Month number ("01" .. "12").
     */
    private String lookup(String month) {

        int index;
        try {
            index = Integer.parseInt(month) - 1;
        } catch (Throwable t) {
            index = 0;  // Can not happen, in theory
        }
        return (months[index]);

    }


    /**
     * Open the new log file for the date specified by <code>dateStamp</code>.
     */
    private synchronized void open() {

        // Create the directory if necessary
        File dir = new File(directory);
        if (!dir.isAbsolute())
            dir = new File(System.getProperty("catalina.base"), directory);
        dir.mkdirs();

        // Open the current log file
        try {
            String pathname;
            // If no rotate - no need for dateStamp in fileName
            if (rotatable){
                pathname = dir.getAbsolutePath() + File.separator +
                            prefix + dateStamp + suffix;
            } else {
                pathname = dir.getAbsolutePath() + File.separator +
                            prefix + suffix;
            }
            writer = new PrintWriter(new FileWriter(pathname, true), true);
        } catch (IOException e) {
            writer = null;
        }

    }


    /**
     * Return the replacement text for the specified pattern character.
     *
     * @param pattern Pattern character identifying the desired text
     * @param date the current Date so that this method doesn't need to
     *        create one
     * @param request Request being processed
     * @param response Response being processed
     */
    private String replace(char pattern, Date date, Request request,
                           Response response, long time) {

        String value = null;

        ServletRequest req = request.getRequest();
        HttpServletRequest hreq = (HttpServletRequest) req;
        ServletResponse res = response.getResponse();
        HttpServletResponse hres = (HttpServletResponse) res;

        if (pattern == 'a') {
            value = req.getRemoteAddr();
        } else if (pattern == 'A') {
            try {
                value = InetAddress.getLocalHost().getHostAddress();
            } catch(Throwable e){
                value = "127.0.0.1";
            }
        } else if (pattern == 'b') {
            int length = response.getContentCount();
            if (length <= 0)
                value = "-";
            else
                value = "" + length;
        } else if (pattern == 'B') {
            value = "" + response.getContentLength();
        } else if (pattern == 'h') {
            value = req.getRemoteHost();
        } else if (pattern == 'H') {
            value = req.getProtocol();
        } else if (pattern == 'l') {
            value = "-";
        } else if (pattern == 'm') {
            if (hreq != null)
                value = hreq.getMethod();
            else
                value = "";
        } else if (pattern == 'p') {
            value = "" + req.getServerPort();
        } else if (pattern == 'D') {
                    value = "" + time;
        } else if (pattern == 'q') {
            String query = null;
            if (hreq != null)
                query = hreq.getQueryString();
            if (query != null)
                value = "?" + query;
            else
                value = "";
        } else if (pattern == 'r') {
            StringBuffer sb = new StringBuffer();
            if (hreq != null) {
                sb.append(hreq.getMethod());
                sb.append(space);
                sb.append(hreq.getRequestURI());
                if (hreq.getQueryString() != null) {
                    sb.append('?');
                    sb.append(hreq.getQueryString());
                }
                sb.append(space);
                sb.append(hreq.getProtocol());
            } else {
                sb.append("- - ");
                sb.append(req.getProtocol());
            }
            value = sb.toString();
        } else if (pattern == 'S') {
            if (hreq != null)
                if (hreq.getSession(false) != null)
                    value = hreq.getSession(false).getId();
                else value = "-";
            else
                value = "-";
        } else if (pattern == 's') {
            if (hres != null)
                value = "" + ((HttpResponse) response).getStatus();
            else
                value = "-";
        } else if (pattern == 't') {
            StringBuffer temp = new StringBuffer("[");
            temp.append(dayFormatter.format(date));             // Day
            temp.append('/');
            temp.append(lookup(monthFormatter.format(date)));   // Month
            temp.append('/');
            temp.append(yearFormatter.format(date));            // Year
            temp.append(':');
            temp.append(timeFormatter.format(date));            // Time
            temp.append(' ');
            temp.append(timeZone);                              // Timezone
            temp.append(']');
            value = temp.toString();
        } else if (pattern == 'T') {
            value = timeTakenFormatter.format(time/1000d);
        } else if (pattern == 'u') {
            if (hreq != null)
                value = hreq.getRemoteUser();
            if (value == null)
                value = "-";
        } else if (pattern == 'U') {
            if (hreq != null)
                value = hreq.getRequestURI();
            else
                value = "-";
        } else if (pattern == 'v') {
            value = req.getServerName();
        } else {
            value = "???" + pattern + "???";
        }

        if (value == null)
            return ("");
        else
            return (value);

    }


    /**
     * Return the replacement text for the specified "header/parameter".
     *
     * @param header The header/parameter to get
     * @param type Where to get it from i=input,c=cookie,r=ServletRequest,s=Session
     * @param request Request being processed
     * @param response Response being processed
     */
    private String replace(String header, char type, Request request,
                           Response response) {

        Object value = null;

        ServletRequest req = request.getRequest();
        HttpServletRequest hreq = (HttpServletRequest) req;

        switch (type) {
            case 'i':
                if (null != hreq)
                    value = hreq.getHeader(header);
                else
                    value= "??";
                break;
/*
            // Someone please make me work
            case 'o':
                break;
*/
            case 'c':
                 Cookie[] c = hreq.getCookies();
                 for (int i=0; c != null && i < c.length; i++){
                     if (header.equals(c[i].getName())){
                         value = c[i].getValue();
                         break;
                     }
                 }
                break;
            case 'r':
                if (null != hreq)
                    value = hreq.getAttribute(header);
                else
                    value= "??";
                break;
            case 's':
                if (null != hreq) {
                    HttpSession sess = hreq.getSession(false);
                    if (null != sess)
                        value = sess.getAttribute(header);
                }
               break;
            default:
                value = "???";
        }

        /* try catch in case toString() barfs */
        try {
            if (value!=null)
                if (value instanceof String)
                    return (String)value;
                else
                    return value.toString();
            else
               return "-";
        } catch(Throwable e) {
            return "-";
        }
    }


    /**
     * This method returns a Date object that is accurate to within one
     * second.  If a thread calls this method to get a Date and it's been
     * less than 1 second since a new Date was created, this method
     * simply gives out the same Date again so that the system doesn't
     * spend time creating Date objects unnecessarily.
     *
     * @return Date
     */
    private Date getDate() {
        if(currentDate == null) {
	    currentDate = new Date();
        } else {
          // Only create a new Date once per second, max.
          long systime = System.currentTimeMillis();
          if ((systime - currentDate.getTime()) > 1000) {
              currentDate = new Date(systime);
          }
	}

        return currentDate;
    }


    private String calculateTimeZoneOffset(long offset) {
        StringBuffer tz = new StringBuffer();
        if ((offset<0))  {
            tz.append("-");
            offset = -offset;
        } else {
            tz.append("+");
        }

        long hourOffset = offset/(1000*60*60);
        long minuteOffset = (offset/(1000*60)) % 60;

        if (hourOffset<10)
            tz.append("0");
        tz.append(hourOffset);

        if (minuteOffset<10)
            tz.append("0");
        tz.append(minuteOffset);

        return tz.toString();
    }


    // ------------------------------------------------------ Lifecycle Methods


    /**
     * Add a lifecycle event listener to this component.
     *
     * @param listener The listener to add
     */
    public void addLifecycleListener(LifecycleListener listener) {

        lifecycle.addLifecycleListener(listener);

    }


    /**
     * Get the lifecycle listeners associated with this lifecycle. If this
     * Lifecycle has no listeners registered, a zero-length array is returned.
     */
    public LifecycleListener[] findLifecycleListeners() {

        return lifecycle.findLifecycleListeners();

    }


    /**
     * Remove a lifecycle event listener from this component.
     *
     * @param listener The listener to add
     */
    public void removeLifecycleListener(LifecycleListener listener) {

        lifecycle.removeLifecycleListener(listener);

    }


    /**
     * Prepare for the beginning of active use of the public methods of this
     * component.  This method should be called after <code>configure()</code>,
     * and before any of the public methods of the component are utilized.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that prevents this component from being used
     */
    public void start() throws LifecycleException {

        // Validate and update our current component state
        if (started)
            throw new LifecycleException
                (sm.getString("accessLogValve.alreadyStarted"));
        lifecycle.fireLifecycleEvent(START_EVENT, null);
        started = true;

        // Initialize the timeZone, Date formatters, and currentDate
        TimeZone tz = TimeZone.getDefault();
        timeZone = calculateTimeZoneOffset(tz.getRawOffset());

        if (fileDateFormat==null || fileDateFormat.length()==0)
            fileDateFormat = "yyyy-MM-dd";
        dateFormatter = new SimpleDateFormat(fileDateFormat);
        dateFormatter.setTimeZone(tz);
        dayFormatter = new SimpleDateFormat("dd");
        dayFormatter.setTimeZone(tz);
        monthFormatter = new SimpleDateFormat("MM");
        monthFormatter.setTimeZone(tz);
        yearFormatter = new SimpleDateFormat("yyyy");
        yearFormatter.setTimeZone(tz);
        timeFormatter = new SimpleDateFormat("HH:mm:ss");
        timeFormatter.setTimeZone(tz);
        currentDate = new Date();
        dateStamp = dateFormatter.format(currentDate);
        timeTakenFormatter = new DecimalFormat("0.000");

        open();

    }


    /**
     * Gracefully terminate the active use of the public methods of this
     * component.  This method should be the last one called on a given
     * instance of this component.
     *
     * @exception LifecycleException if this component detects a fatal error
     *  that needs to be reported
     */
    public void stop() throws LifecycleException {

        // Validate and update our current component state
        if (!started)
            throw new LifecycleException
                (sm.getString("accessLogValve.notStarted"));
        lifecycle.fireLifecycleEvent(STOP_EVENT, null);
        started = false;

        close();

    }
}
