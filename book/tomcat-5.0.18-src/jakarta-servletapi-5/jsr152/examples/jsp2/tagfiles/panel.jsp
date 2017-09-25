<%@ taglib prefix="tags" tagdir="/WEB-INF/tags" %>
<html>
  <head>
    <title>JSP 2.0 Examples - Panels using Tag Files</title>
  </head>
  <body>
    <h1>JSP 2.0 Examples - Panels using Tag Files</h1>
    <hr>
    <p>This JSP page invokes a custom tag that draws a 
    panel around the contents of the tag body.  Normally, such a tag 
    implementation would require a Java class with many println() statements,
    outputting HTML.  Instead, we can use a .tag file as a template,
    and we don't need to write a single line of Java or even a TLD!</p>
    <hr>
    <table border="0">
      <tr valign="top">
        <td>
          <tags:panel color="#ff8080" bgcolor="#ffc0c0" title="Panel 1">
	    First panel.<br/>
	  </tags:panel>
        </td>
        <td>
          <tags:panel color="#80ff80" bgcolor="#c0ffc0" title="Panel 2">
	    Second panel.<br/>
	    Second panel.<br/>
	    Second panel.<br/>
	    Second panel.<br/>
	  </tags:panel>
        </td>
        <td>
          <tags:panel color="#8080ff" bgcolor="#c0c0ff" title="Panel 3">
	    Third panel.<br/>
            <tags:panel color="#ff80ff" bgcolor="#ffc0ff" title="Inner">
	      A panel in a panel.
	    </tags:panel>
	    Third panel.<br/>
	  </tags:panel>
        </td>
      </tr>
    </table>
  </body>
</html>
