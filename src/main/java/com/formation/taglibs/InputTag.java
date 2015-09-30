package com.formation.taglibs;

import java.io.IOException;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.log4j.Logger;

import com.formation.archetypes.ActionForm;
import com.formation.exceptions.runtime.NoGetterMethodFoundForProvidedFormInputNameException;
import com.formation.exceptions.runtime.NoSetterMethodFoundForProvidedFormInputNameException;

public class InputTag extends TagSupport
{
    private String name;
    private String value;
    private Logger logger = Logger.getLogger(this.getClass());

    @Override
    public int doStartTag() throws JspException
    {
        JspWriter out = pageContext.getOut();
        HttpSession httpSession = pageContext.getSession();

        Class formClass = (Class) httpSession.getAttribute("formClass");
        String formClassFullName = formClass.getCanonicalName();
        String getterName = "get" + name.substring(0, 1).toUpperCase() + name.substring(1, name.length());
        String setterName = "set" + name.substring(0, 1).toUpperCase() + name.substring(1, name.length());

        if (httpSession.getAttribute(formClassFullName) == null)
        {
            // si l'actionform n'est pas instancié
            // vérifie si le champ est dans l'actionForm avec son setter et son
            // getter

            try
            {
                Method getter = formClass.getMethod(getterName, new Class[] {});
            }
            catch (NoSuchMethodException e)
            {
                throw new NoGetterMethodFoundForProvidedFormInputNameException("The ActionForm " + formClass.getCanonicalName() + " field " + name + " does not have a getter method matching his name. The convention for a field named myField is to call the getter method getMyField. Please check the getter method name or the field name." + "\n" + e.getMessage());
            }
            catch (SecurityException e)
            {
                logger.error(e.getMessage());
            }

            try
            {
                Method setter = formClass.getMethod(setterName, new Class[]
                {
                    // pas génial. J'aurais du chercher le field et en prendre
                    // le type, comme dans le Populate.
                    String.class
                });
            }
            catch (NoSuchMethodException e)
            {
                throw new NoSetterMethodFoundForProvidedFormInputNameException("The ActionForm " + formClass.getCanonicalName() + " field " + name + " does not have a setter method matching his name. The convention for a field named myField is to call the setter method setMyField. Please check the setter method name or the field name." + "\n" + e.getMessage());
            }
            catch (SecurityException e)
            {
                logger.error(e.getMessage());
            }

            // si c'es le cas on est encore là et on affiche le render html

            try
            {
                out.println("<input type='text' name='" + name + "'/>");
            }
            catch (IOException e)
            {
                logger.error(e.getMessage());
            }

        }
        else
        {
            ActionForm actionForm = (ActionForm) httpSession.getAttribute(formClassFullName);
            Method getter=null;

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
            try
            {
                value = (String) getter.invoke(actionForm, new Object[] {});
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
            if (value == null)
            {
                value = "";
            }
            try
            {
                out.println("<input type='text' name='" + name + "' value='" + value + "'/>");
            }
            catch (IOException e)
            {
                logger.error(e.getMessage());
            }

        }
        // sinon charge les valeurs présentes dans l'actionForm

        return javax.servlet.jsp.tagext.Tag.SKIP_BODY;
    }

    public void setName(String name)
    {
        this.name = name;
    }

    public String getName()
    {
        return name;
    }

    public String getValue()
    {
        return value;
    }

    public void setValue(String value)
    {
        this.value = value;
    }

}
