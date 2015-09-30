package com.formation.taglibs;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.log4j.Logger;

import com.formation.archetypes.ActionForm;
import com.formation.exceptions.runtime.NoGetterMethodFoundForProvidedFormInputNameException;

/**
 * Tag &lt;tt:property name=""&gt; qui va permettre d'afficher une valeur de
 * l'ActionForm en lui spécifiant son nom.
 * @author filippo
 */
public class PropertyTag extends TagSupport
{

    /**
     * N° de version.
     */
    private static final long serialVersionUID = 1L;
    /**
     * Nom de l'attribut de l'ActionForm que l'on souhaite afficher.
     */
    private String name;
    /**
     * Logger.
     */
    private Logger logger = Logger.getLogger(this.getClass());

    @Override
    public int doStartTag() throws JspException
    {
        JspWriter out = pageContext.getOut();

        ActionForm aForm = (ActionForm) pageContext.getRequest().getAttribute("monForm");

        String getterName = "get" + name.substring(0, 1).toUpperCase() + name.substring(1, name.length());
        Class formClass = aForm.getClass();

        Method getter = null;
        try
        {
            getter = formClass.getMethod(getterName, new Class[] {});
        }
        catch (NoSuchMethodException e)
        {
            throw new NoGetterMethodFoundForProvidedFormInputNameException("The ActionForm " + formClass.getCanonicalName() + " field " + name + " does not have a getter method matching his name. The convention for a field named myField is to call the getter method getMyField. Please check the getter method name or the field name." + "\n" + e.getMessage());
        }
        catch (SecurityException e)
        {
            logger.error(e.getMessage());
        }
        Object getValue = appelDuGetter(aForm, getter);
        try
        {
            out.print(getValue.toString());
        }
        catch (IOException e)
        {
            logger.error(e.getMessage());
        }
        return javax.servlet.jsp.tagext.Tag.SKIP_BODY;
    }

    /**
     * Setter du name.
     * @param pName
     *        Nom de l'attribut de l'ActionForm que l'on veut récupérer.
     */
    public void setName(String pName)
    {
        this.name = pName;
    }

    /**
     * Appel du getter passé en argument de l'actionForm indiqué.
     * @param aForm
     *        ActionForm dont on veut appeler le getter.
     * @param m
     *        Getter que l'on souhaite appeler.
     * @return La valeur retournée par le getter.
     */
    private Object appelDuGetter(ActionForm aForm, Method m)
    {
        Object getValue = null;
        try
        {
            getValue = m.invoke(aForm, new Object[] {});
        }
        catch (IllegalAccessException e)
        {
            logger.error(e.getMessage());
        }
        catch (IllegalArgumentException e)
        {
            logger.error(e.getMessage());
        }
        catch (InvocationTargetException e)
        {
            logger.error(e.getMessage());
        }
        return getValue;

    }

}
