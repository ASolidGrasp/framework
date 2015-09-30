package com.formation.taglibs;

import java.io.IOException;
import java.util.Map;
import java.util.Map.Entry;

import javax.servlet.http.HttpSession;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.TagSupport;

import org.apache.log4j.Logger;

import com.formation.archetypes.ActionForm;
import com.formation.configreader.ConfigurationReader;
import com.formation.exceptions.runtime.WrongActionCanonicalNameSpecifiedException;
import com.formation.exceptions.runtime.WrongActionFormCanonicalNameSpecifiedException;
import com.formation.factory.Factory;

public class FormTag extends TagSupport
{
    private String action;
    private HttpSession httpSession;
    private Map<String, String[]> actionsAndFormsMap;
    private Class formClass;
    private String actionClassFullName;
    private String formClassFullName;
    private boolean actionClassFound = false;
    private boolean formClassFound = false;
    private Logger logger = Logger.getLogger(this.getClass());

    @Override
    public int doStartTag() throws JspException
    {
        JspWriter out = pageContext.getOut();

        // TO DO :
        // mettre la classe de l'actionForm en session pour que les input
        // puissent y
        // accéder

        ConfigurationReader configurationReader = ConfigurationReader.getReader();
        if (actionsAndFormsMap == null)
        {
            actionsAndFormsMap = configurationReader.buildActionsAndFormsMap();
        }
        String[] actionAndFormFullNames = configurationReader.getActionAndFormCanonicalNamesByAction(actionsAndFormsMap, "/" + action);

        if (actionAndFormFullNames != null)
        {
            // Si l'action renseignée par l'utilisateur a été trouvée dans
            // son fichier de configuration

            actionClassFullName = actionAndFormFullNames[0];
            formClassFullName = actionAndFormFullNames[1];

            // on vérifie que les classes Action et ActionForm requêtées
            // existent sans les instancier

            try
            {
                Class.forName(actionClassFullName);
                actionClassFound = true;
            }
            catch (ClassNotFoundException e)
            {
                throw new WrongActionCanonicalNameSpecifiedException("Whether the Action class full name " + actionClassFullName + " you specified in the configuration file is incorrect. Please check it. Or its class can't be loaded." + "\n" + e.getMessage());
            }

            try
            {
                formClass = Class.forName(formClassFullName);
                formClassFound = true;
                pageContext.getSession().setAttribute("formClass", formClass);
            }
            catch (ClassNotFoundException e)
            {
                throw new WrongActionFormCanonicalNameSpecifiedException("Whether the ActionForm class full name " + formClassFullName + " you specified in the configuration file is incorrect. Please check it. Or its class can't be loaded." + "\n" + e.getMessage());
            }
            try
            {
                out.print("<form method='POST' action='" + action + "'>");
            }
            catch (IOException e)
            {
                logger.error(e.getMessage());
            }
        }

        return EVAL_BODY_INCLUDE;
    }

    @Override
    public int doEndTag() throws JspException
    {
        // TODO on instancie ou récupère l'instance de l'actionform et on charge
        // ses valeurs par défaut
        if (actionClassFound && formClassFound)
        {
            Factory factory = Factory.getFactory();
            httpSession = pageContext.getSession();

            // instanciation de l'action form
            ActionForm myForm;

            // m'assure que l'actionForm est instancié et qu'il est en session
            // pourquoi ??????????????????????????????????? dans quel but ?
            // if (httpSession.getAttribute(formClassFullName) == null)
            // {
            // // s'il n'y a pas d'instance en session on la crée
            // myForm = factory.getInstance(formClassFullName);
            // // et on la met en session pour la prochaine fois
            // httpSession.setAttribute(formClassFullName, myForm);
            // }

            JspWriter out = pageContext.getOut();
            try
            {
                out.print("</form>");
            }
            catch (IOException e)
            {
                logger.error(e.getMessage());
            }
        }
        return SKIP_BODY;

    }

    public String getAction()
    {
        return action;
    }

    public void setAction(String action)
    {
        this.action = action;
    }

}
