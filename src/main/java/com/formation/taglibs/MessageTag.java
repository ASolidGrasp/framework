package com.formation.taglibs;

import java.io.IOException;
import java.io.StringWriter;

import javax.servlet.jsp.JspContext;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.SimpleTagSupport;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.log4j.Logger;

public class MessageTag extends TagSupport
{
    private Logger logger = Logger.getLogger(this.getClass());

    @Override
    public int doStartTag() throws JspException
    {
        JspWriter out = pageContext.getOut();
        String message = (String) pageContext.getRequest().getAttribute("message");

        try
        {
            if (message != null)
            {
                out.print(message);
            }
        }
        catch (IOException e)
        {
            logger.error(e.getMessage());
        }
        return javax.servlet.jsp.tagext.Tag.SKIP_BODY;
    }

}
