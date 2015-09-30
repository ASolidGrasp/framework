package com.formation.taglibs;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.log4j.Logger;

/**
 * Taglib pour afficher des messages pour l'utilisateur.
 * @author filippo
 */
public class MessageTag extends TagSupport
{
    /**
     * Un logger.
     */
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
