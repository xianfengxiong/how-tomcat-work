/*
 * $Header: /home/cvs/jakarta-tomcat-4.0/webapps/admin/WEB-INF/classes/org/apache/webapp/admin/ActionsTag.java,v 1.4 2002/06/09 19:10:22 remm Exp $
 * $Revision: 1.4 $
 * $Date: 2002/06/09 19:10:22 $
 *
 * ====================================================================
 *
 * The Apache Software License, Version 1.1
 *
 * Copyright (c) 2001 The Apache Software Foundation.  All rights
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
 */


package org.apache.webapp.admin;


import java.io.IOException;
import java.net.URLEncoder;
import java.util.ArrayList;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.BodyTagSupport;


/**
 * <p>JSP custom tag that renders an "instant actions" control.  To the user,
 * it appears as an HTML &lt;select&gt; element (i.e. a combo box), with
 * the behavior of selecting a new page for the current frame or window when
 * a different option is selected, without requiring a submit action.
 * This tag has the following user-settable attributes:</p>
 * <ul>
 * <li><strong>size</strong> - (Integer) number of rows that will be visible
 *     to the user.  If not specified, one row will be visible.</li>
 * <li><strong>style</strong> - The CSS style class to be applied to the
 *     entire rendered output of the instant actions control, if any.</li>
 * </ul>
 *
 * <strong>FIXME</strong> - Internationalize the exception messages!
 *
 * @author Craig R. McClanahan
 * @version $Revision: 1.4 $ $Date: 2002/06/09 19:10:22 $
 */

public class ActionsTag extends BodyTagSupport {


    // ----------------------------------------------------- Manifest Constants


    /**
     * Attribute name used to indicate that we have generated the JavaScript
     * function already on the current page.  The value stored for this
     * attribute is arbitrary - only its existence is relevant.
     */
    protected static final String FUNCTION_TAG =
        "org.apache.webapp.admin.ActionsTag.FUNCTION_TAG";


    // ----------------------------------------------------- Instance Variables


    /**
     * The set of labels for the Actions displayed by this control.
     */
    protected ArrayList labels = new ArrayList();


    /**
     * The set of "selected" flags for Actions displayed by this control.
     */
    protected ArrayList selecteds = new ArrayList();

    /**
     * The set of "disabled" flags for Actions displayed by this control.
     */
    protected ArrayList disableds = new ArrayList();

    /**
     * The set of URLs for the Actions displayed by this control.
     */
    protected ArrayList urls = new ArrayList();


    // ------------------------------------------------------------- Properties


    /**
     * The number of elements that will be displayed to the user.
     */
    protected int size = 1;

    public int getSize() {
        return (this.size);
    }

    public void setSize(int size) {
        this.size = size;
    }


    /**
     * The CSS style class to be applied to the entire rendered output
     * of this "instant actions" object.
     */
    protected String style = null;

    public String getStyle() {
        return (this.style);
    }

    public void setStyle(String style) {
        this.style = style;
    }


    // --------------------------------------------------------- Public Methods


    public int doStartTag() throws JspException {

        this.labels.clear();
        this.selecteds.clear();
        this.urls.clear();

        return (EVAL_BODY_TAG);

    }


    /**
     * Render this instant actions control.
     *
     * @exception JspException if a processing error occurs
     */
    public int doEndTag() throws JspException {

        JspWriter out = pageContext.getOut();

        try {

            // Render (once only) the JavaScript function we need
            if (pageContext.getAttribute(FUNCTION_TAG) == null) {
                out.println();
                out.println("<script language=\"JavaScript\">");
                out.println("<!--");
                out.println("function IA_jumpMenu(targ,selObj) {");
                out.println("  dest = selObj.options[selObj.selectedIndex].value;");
                out.println("  if (dest.length > 0) {");
                out.println("    eval(targ+\".location='\"+dest+\"'\");");
                out.println("  }");
                out.println("}");
                out.println("//-->");
                out.println("</script>");
                out.println();
                pageContext.setAttribute(FUNCTION_TAG, Boolean.TRUE);
            }

            // Render the beginning of this element
            out.println();
            out.print("<select");
            if (size > 1) {
                out.print(" size=\"");
                out.print(size);
                out.print("\"");
            }
            if (style != null) {
                out.print(" class=\"");
                out.print(style);
                out.print("\"");
            }
            out.print(" onchange=\"IA_jumpMenu('self',this)\"");
            out.println(">");

            // Render each defined action
            int n = labels.size();
            for (int i = 0; i < n; i++) {
                String label = (String) labels.get(i);
                boolean selected = ((Boolean) selecteds.get(i)).booleanValue();
                boolean disabled = ((Boolean) disableds.get(i)).booleanValue();             
                String url = (String) urls.get(i);
                out.print("<option");
                if (selected)
                    out.print(" selected=\"selected\"");
                if (disabled)
                    out.print(" disabled=\"true\"");                
                out.print(" value=\"");
                if (url != null)
                    out.print(url);
                out.print("\"");
                out.print(">");
                if (label != null)
                    out.print(label);
                out.println("</option>");
            }

            // Render the end of this element
            out.println("</select>");
            out.println();

        } catch (IOException e) {
            throw new JspException(e);
        }

        return (EVAL_PAGE);

    }


    /**
     * Release all state information set by this tag.
     */
    public void release() {

        this.labels.clear();
        this.selecteds.clear();
        this.urls.clear();

        this.size = 1;
        this.style = null;

    }


    // -------------------------------------------------------- Package Methods


    /**
     * Add a new Action to the set that will be rendered by this control.
     *
     * @param label Localized label visible to the user
     * @param selected Initial selected state of this option
     * @param disabled Ability to be selected state of this option
     * @param url URL to which control should be transferred if selected
     */
    void addAction(String label, boolean selected, boolean disabled, String url) {

        labels.add(label);
        selecteds.add(new Boolean(selected));
        disableds.add(new Boolean(disabled));
        urls.add(url);

    }


    // ------------------------------------------------------ Protected Methods


}
