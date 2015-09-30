package com.formation.taglibs;

import java.io.IOException;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.log4j.Logger;

import com.formation.archetypes.ActionForm;
import com.formation.exceptions.runtime.NameSuppliedInFormInputDoesNotMatchAnyActionFormFieldException;
import com.formation.exceptions.runtime.NoGetterMethodFoundForProvidedFormInputNameException;
import com.formation.exceptions.runtime.NoSetterMethodFoundForProvidedFormInputNameException;
import com.formation.exceptions.runtime.SecurityManagerRuleForbidAccessToFieldException;

/**
 * Tag qui va permettre d'afficher depuis la session les attributs nommés et de
 * les setter dans l'ActionForm lors de la soumission d'un formulaire.
 * @author filippo
 */
public class InputTag extends TagSupport
{
    /**
     * Nom de l'attribut à récupérer ou à setter.
     */
    private String name;
    /**
     * Valeur à afficher lorsque on récupère le champ dans la session.
     */
    private String value;
    /**
     * Logger.
     */
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

            Field f = foundField(formClass);
            Method getter = foundGetter(formClass, getterName);
            Method setter = foundSetter(formClass, setterName, f);

            // si c'es le cas on est encore là et on affiche le render html
            if (f != null && getter != null && setter != null)
            {
                printTagWithoutValue(out);
            }

        }
        else
        {
            ActionForm actionForm = (ActionForm) httpSession.getAttribute(formClassFullName);

            Method getter = foundGetter(formClass, getterName);
            value = callGetter(actionForm, getter);
            if (value == null)
            {
                value = "";
            }
            printTagWithValue(out);
        }
        // sinon charge les valeurs présentes dans l'actionForm

        return javax.servlet.jsp.tagext.Tag.SKIP_BODY;
    }

    /**
     * Essaye de renvoyer la methode du nom indiqué.
     * @param formClass
     *        La classe de l'actionForm où l'on cherche a méthode.
     * @param getterName
     *        Le nom de la méthode cherchée.
     * @return La méthode recherchée ou null si elle n'est pas trouvée.
     */
    private Method foundGetter(Class formClass, String getterName)
    {
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
        return getter;
    }

    /**
     * Essaye de renvoyer la methode du nom indiqué.
     * @param formClass
     *        La classe de l'actionForm où l'on cherche a méthode.
     * @param setterName
     *        Le nom de la méthode cherchée.
     * @param f
     *        L'attribut associé au setter.
     * @return La méthode recherchée ou null si elle n'est pas trouvée.
     */
    private Method foundSetter(Class formClass, String setterName, Field f)
    {
        Method setter = null;
        try
        {
            setter = formClass.getMethod(setterName, new Class[]
            {
                f.getType()
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
        return setter;
    }

    /**
     * Essaye de renvoyer l'attribut du nom indiqué.
     * @param formClass
     *        L'ActionForm où l'attribut est cherché
     * @return L'attribut cherché ou null s'il n'est pas trouvé.
     */
    private Field foundField(Class formClass)
    {
        Field f = null;
        try
        {
            f = formClass.getDeclaredField(name);
        }
        catch (NoSuchFieldException e1)
        {
            throw new NameSuppliedInFormInputDoesNotMatchAnyActionFormFieldException("The name " + name + " provided to a form input has not matching field in the ActionForm " + "\n" + e1.getMessage());
        }
        catch (SecurityException e1)
        {
            throw new SecurityManagerRuleForbidAccessToFieldException("A rule set with the Security Manager forbid access to the " + name + " field in the " + formClass.getCanonicalName() + " actionForm." + "\n" + e1.getMessage());
        }
        return f;
    }

    /**
     * Appellele getter pour récupérer la valeur précédemment sauvée dans
     * l'actionForm après soumission d'un formulaire.
     * @param actionForm
     *        L'actionForm possédant l'attribut et le getter pour le récupérer.
     * @param getter
     *        Méthode getter.
     * @return La valeur récupérée par le getter.
     */
    private String callGetter(ActionForm actionForm, Method getter)
    {
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
        return value;
    }

    /**
     * Affiche la sortie HTML équivalente au taglib insérée quand il n'y a pas
     * d'ActionForm instancié (la première fois que l'on affiche le formulaire).
     * @param out
     *        Le writer permettant d'écrire dans la page JSP.
     */
    private void printTagWithoutValue(JspWriter out)
    {
        try
        {
            out.println("<input type='text' name='" + name + "'/>");
        }
        catch (IOException e)
        {
            logger.error(e.getMessage());
        }
    }

    /**
     * Affiche la sortie HTML équivalente au taglib insérée quand il y a un
     * ActionForm instancié (et donc une valeur à récupérer).
     * @param out
     *        Le writer permettant d'écrire dans la page JSP.
     */
    private void printTagWithValue(JspWriter out)
    {
        try
        {
            out.println("<input type='text' name='" + name + "' value='" + value + "'/>");
        }
        catch (IOException e)
        {
            logger.error(e.getMessage());
        }
    }

    /**
     * Setter du nom.
     * @param pName
     *        Nom à setter.
     */
    public void setName(String pName)
    {
        this.name = pName;
    }

    /**
     * Getter du nom.
     * @return le nom demandé.
     */
    public String getName()
    {
        return name;
    }

    /**
     * Getter de la valeur.
     * @return La valeur demandée.
     */
    public String getValue()
    {
        return value;
    }

    /**
     * Setter de la valeur.
     * @param pValue
     *        La valeur à setter.
     */
    public void setValue(String pValue)
    {
        this.value = pValue;
    }

}
