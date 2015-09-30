package com.formation.populate;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.log4j.Logger;

import com.formation.exceptions.runtime.NameSuppliedInFormInputDoesNotMatchAnyActionFormFieldException;
import com.formation.exceptions.runtime.NoSetterMethodFoundForProvidedFormInputNameException;
import com.formation.exceptions.runtime.SecurityManagerRuleForbidAccessToFieldException;

/**
 * Classe qui va setter les atributs de l'ActionForm avec les valeurs passées
 * par parametersMap qui est une map des paramètres de la requête, issus du
 * formulaire soumis.
 * @author filippo
 */
public class FormFiller
{
    /**
     * Instance unique du FormFiller.
     */
    private static FormFiller formFillerInstance = new FormFiller();

    /**
     * Constructeur privé pour garantir l'unicité de l'instance.
     */
    private FormFiller()
    {
    }

    /**
     * Seul accès public à l'instance de cete classe. Cet accès ne permet pas
     * d'instancier mais de récupérer l'instance créée au chargement de la
     * classe.
     * @return L'instance unique du FormFiller
     */
    public static FormFiller getFormFiller()
    {
        return formFillerInstance;
    }

    /**
     * Logger.
     */
    private Logger logger = Logger.getLogger(this.getClass());

    /**
     * Méthode qui va setter les atributs de l'ActionForm avec les valeurs
     * passées par parametersMap qui est une map des paramètres de la requête,
     * issus du formulaire soumis.
     * @param actionForm
     *        ActionForm dont on doit setter les attributs.
     * @param parametersMap
     *        Map des paramètres de la requête reçue.
     */
    public void populateBean(Object actionForm, Map<String, String[]> parametersMap)
    {
        Class<?> c = actionForm.getClass();

        for (Entry<String, String[]> parameterEntry : parametersMap.entrySet())
        {
            String parameterName = parameterEntry.getKey();
            String parameterValue = parameterEntry.getValue()[0];

            Field f = getFieldInClassByName(c, parameterName);
            setFieldInActionFormToValue(actionForm, f, parameterValue);
        }
    }

    /**
     * Retourne l'attribut dont on spécifie le nom dans la classe spécifiée.
     * @param c
     *        Classe spécifiée.
     * @param name
     *        Nom de l'attribut souhaité.
     * @return L'attribut de la classe indiquée avec le nom indiqué.
     */
    private Field getFieldInClassByName(Class<?> c, String name)
    {
        Field f;
        try
        {
            f = c.getDeclaredField(name);
        }
        catch (NoSuchFieldException e)
        {
            throw new NameSuppliedInFormInputDoesNotMatchAnyActionFormFieldException("The name " + name + " provided to a form input has not matching field in the ActionForm ");
        }
        catch (SecurityException e)
        {
            throw new SecurityManagerRuleForbidAccessToFieldException("A rule set with the Security Manager forbid access to the " + name + " field in the " + c.getCanonicalName() + " actionForm." + "\n" + e.getMessage());
        }
        return f;
    }

    /**
     * Invoke le setter de l'attribut indiqué de l'objet indiqué à la valeur
     * indiquée.
     * @param actionForm
     *        ActionForm contenant l'attribut à setter.
     * @param f
     *        Attribut à setter
     * @param value
     *        Valeur à attribuer à l'attribut.
     */
    private void setFieldInActionFormToValue(Object actionForm, Field f, String value)
    {
        String setterName = "set" + f.getName().substring(0, 1).toUpperCase() + f.getName().substring(1, f.getName().length());
        Class<?> c = actionForm.getClass();
        Class<?>[] parameterTypes = new Class[]
        {
            f.getType()
        };
        Object[] parameterValues = new Object[]
        {
            value
        };
        Method setter;
        try
        {
            setter = c.getMethod(setterName, parameterTypes);
            setter.invoke(actionForm, parameterValues);
        }
        catch (NoSuchMethodException e)
        {
            throw new NoSetterMethodFoundForProvidedFormInputNameException("The ActionForm " + c.getCanonicalName() + " field " + f.getName() + " does not have a setter method matching is name. The convention for a field named myField is to call the setter method setMyField. Please check the setter method name." + "\n" + e.getMessage());
        }
        catch (SecurityException e)
        {
            logger.error("A rule set with the Security Manager forbid access to the " + setterName + " method in the " + c.getCanonicalName() + " actionForm." + "\n" + e.getMessage());
        }
        catch (IllegalAccessException e)
        {
            logger.error("The actionForm " + c.getCanonicalName() + " setter method " + setterName + " can' be accessed" + "\n" + e.getMessage());
        }
        catch (IllegalArgumentException e)
        {
            logger.error(e.getMessage());
        }
        catch (InvocationTargetException e)
        {
            logger.error(e.getMessage());
        }
    }

}
