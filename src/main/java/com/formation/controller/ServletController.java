package com.formation.controller;

import java.io.IOException;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import com.formation.archetypes.Action;
import com.formation.archetypes.ActionForm;
import com.formation.configreader.ConfigurationReader;
import com.formation.exceptions.runtime.WrongActionCanonicalNameSpecifiedException;
import com.formation.exceptions.runtime.WrongActionFormCanonicalNameSpecifiedException;
import com.formation.factory.Factory;
import com.formation.populate.FormFiller;

/**
 * Définition de l'url-pattern qui va être interceptée par la servlet.
 * @author filippo
 */
@WebServlet(urlPatterns =
{
    "*.do"
})
public class ServletController extends HttpServlet
{
    /**
     * N°de la version.
     */
    private static final long serialVersionUID = 1L;
    /**
     * Map des actions et de leurs actionForm associés avec pour clef
     * l'url-pattern leur étant associée.
     */
    private Map<String, String[]> actionsAndFormsMap;
    /**
     * La session HTTP.
     */
    private HttpSession httpSession;

    /**
     * Déclaration de la factory dont on va se servir pour instancier les Action
     * et les ActionForm.
     */
    private Factory factory;
    /**
     * Déclaration du lecteur de configuration qui peut lire la configuration
     * soit :
     * <ul>
     * <li>dans le fichier de configuration classique ou toute classe Action et
     * ActionForm est déclarée individuellement.</li>
     * <li>à l'aide d'annotations en scannant les packages indiqués dans un
     * fichier de configuration simplifié</li>
     * </ul>
     */
    private ConfigurationReader configurationReader;

    /**
     * Charge la configuration à l'instanciation de la servlet pour ne pas
     * ralentir le premier appel du contrôleur.
     */
    @Override
    public void init() throws ServletException
    {
        // TODO Auto-generated method stub
        super.init();
        factory = Factory.getFactory();

        configurationReader = ConfigurationReader.getReader();
        if (actionsAndFormsMap == null)
        {
            actionsAndFormsMap = configurationReader.buildActionsAndFormsMap();
        }
    }

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        doPost(request, response);
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException
    {
        String actionPath = request.getServletPath();

        String[] refererParts = request.getHeader("referer").split("/");
        String refererPath = "/" + refererParts[refererParts.length - 1];

        String[] actionAndFormFullNames = configurationReader.getActionAndFormCanonicalNamesByAction(actionsAndFormsMap, actionPath);
        if (actionAndFormFullNames != null)
        {
            // Si l'action renseignée par l'utilisateur a été trouvée dans son
            // fichier de configuration

            String actionClassFullName = actionAndFormFullNames[0];
            String formClassFullName = actionAndFormFullNames[1];

            boolean actionClassFound = doesActionClassExist(actionClassFullName);
            boolean formClassFound = doesActionFormClassExist(formClassFullName);
            // on vérifie que les classes Action et ActionForm requêtées
            // existent sans les instancier

            if (actionClassFound && formClassFound)
            {

                httpSession = request.getSession();

                FormFiller populator = FormFiller.getFormFiller();

                // s'il n'y a pas d'instance en session on la crée
                // et on la met en session pour la prochaine fois
                // instanciation de l'action form
                ActionForm myForm = getFormInstanceByClassFullNameFromSesionOrFactory(formClassFullName);

                // on peuple l'ActionForm avec les donnée entrées dans le
                // formulaire
                populator.populateBean(myForm, request.getParameterMap());

                // on met le formulaire en mémoire pour pouvoir y acéder depuis
                // la page cible
                request.setAttribute("monForm", myForm);

                if (myForm.validate(request))
                {
                    // si les données entrées dans le formulaire sont valides
                    // on execute l'action

                    // instanciation de l'action
                    Action myAction = getActionFormInstanceByClassFullNameFromSesionOrFactory(actionClassFullName);

                    // on redirige vers la page spécifiée par l'utilisateur dans
                    // son Action
                    RequestDispatcher rD = request.getRequestDispatcher(myAction.execute(request, response));
                    rD.forward(request, response);
                }
                else
                {
                    // si les données entrées dans le formulaire ne sont pas
                    // valides
                    String message = "Veuillez remplir tous les champs du formulaire";
                    request.setAttribute("message", message);
                    // on le redirige vers la page source avec un message le
                    // notifiant
                    RequestDispatcher rD = request.getRequestDispatcher(refererPath);
                    rD.forward(request, response);
                }
            }
            else
            {
                // si Action ou ActionForm n'est pas trouvée on redirige vers la
                // page d'origine avec un message donant le détail des classes
                // qui manquent
                String message = setMessageDependingOnWhatsMissing(formClassFound, actionClassFound, actionClassFullName, formClassFullName);

                request.setAttribute("message", message);
                RequestDispatcher rD = request.getRequestDispatcher(refererPath);
                rD.forward(request, response);
            }
        }
        else
        {
            String message = "L'action que vous avez spécifié n'a pas été trouvée dans votre fichier de configuration ou le form associé est manquant.";
            request.setAttribute("message", message);
            RequestDispatcher rD = request.getRequestDispatcher(refererPath);
            rD.forward(request, response);
        }
    }

    /**
     * Indique à l'utilisateur du framework si des Action ou ActionForm indiqués
     * dans le fichier de config n'ont pas été trouvés.
     * @param formClassFound
     *        Vrai si la classe de l'ActionForm correspondant à l'action
     *        demandée n'est pas trouvé.
     * @param actionClassFound
     *        Vrai si la classe de l'Action correspondant à l'action demandée
     *        n'est pas trouvée.
     * @param actionClassFullName
     *        Nom complet de la classe Action correspondant à l'action demandée
     *        tel qu'il a été indiqué dans le fichier de configuration.
     * @param formClassFullName
     *        Nom complet de la classe ActionForm correspondant à l'action
     *        demandée tel qu'il a été indiqué dans le fichier de configuration.
     * @return Le message approprié à ce qui n'a pas été trouvé.
     */
    private String setMessageDependingOnWhatsMissing(boolean formClassFound, boolean actionClassFound, String actionClassFullName, String formClassFullName)
    {
        String message;
        if (!formClassFound)
        {
            message = "La classe " + formClassFullName + " n'a pas été trouvée. Veuillez vérifier la correspondance entre votre fichier de configuration et vos classes";
        }
        else if (!actionClassFound)
        {
            message = "La classe " + actionClassFullName + " n'a pas été trouvée. Veuillez vérifier la correspondance entre votre fichier de configuration et vos classes";
        }
        else
        {
            message = "Les classes " + actionClassFullName + " et " + formClassFullName + " n'ont pas été trouvées. Veuillez vérifier la correspondance entre votre fichier de configuration et vos classes";
        }
        return message;
    }

    /**
     * Récupère une instance de l'ActionForm demandé soit depuis la session, si
     * celui-ci a déjà été instancié, soit de la Factory.
     * @param formClassFullName
     *        Nom complet de la classe ActionForm.
     * @return Une instance de l'ActionForm correspondant à l'action demandée.
     */
    private ActionForm getFormInstanceByClassFullNameFromSesionOrFactory(String formClassFullName)
    {
        ActionForm myForm = null;
        if (httpSession.getAttribute(formClassFullName) == null)
        {
            try
            {
                myForm = factory.getInstance(formClassFullName);
                httpSession.setAttribute(formClassFullName, myForm);
            }
            catch (ClassNotFoundException e)
            {
                throw new WrongActionFormCanonicalNameSpecifiedException("Whether the ActionForm class full name " + formClassFullName + " you specified in the configuration file is incorrect. Please check it. Or its class can't be loaded." + "\n" + e.getMessage());
            }
        }
        else
        {
            myForm = (ActionForm) httpSession.getAttribute(formClassFullName);
        }
        return myForm;
    }

    /**
     * Récupère une instance de l'Action demandée soit depuis la session, si
     * celle-ci a déjà été instanciée, soit de la Factory.
     * @param actionClassFullName
     *        Nom complet de la classe Action.
     * @return Une instance de l'Action correspondant à l'action demandée.
     */
    private Action getActionFormInstanceByClassFullNameFromSesionOrFactory(String actionClassFullName)
    {
        Action myAction;
        if (httpSession.getAttribute(actionClassFullName) == null)
        {
            // s'il n'y a pas d'instance pour l'action
            try
            {
                // on la crée
                myAction = factory.getInstance(actionClassFullName);
                // et on la mémorise en session pour la prochaine
                // fois
                httpSession.setAttribute(actionClassFullName, myAction);
            }
            catch (ClassNotFoundException e)
            {
                throw new WrongActionCanonicalNameSpecifiedException("The Action class full name " + actionClassFullName + " you specified in the configuration file is incorrect. Please check it. Or its class can't be loaded." + "\n" + e.getMessage());
            }
        }
        else
        {
            myAction = (Action) httpSession.getAttribute(actionClassFullName);
        }
        return myAction;
    }

    /**
     * Vérifie si la classe Action correspondant à l'action demandée existe.
     * @param actionClassFullName
     *        Nom complet de la classe de l'Action correspondant à l'action
     *        demandée.
     * @return Vrai si la classe Action existe, faux autrement.
     */
    private boolean doesActionClassExist(String actionClassFullName)
    {
        boolean actionClassFound = false;
        try
        {
            Class.forName(actionClassFullName);
            actionClassFound = true;
        }
        catch (ClassNotFoundException e)
        {
            throw new WrongActionCanonicalNameSpecifiedException("The Action class full name " + actionClassFullName + " you specified in the configuration file is incorrect. Please check it. Or its class can't be loaded." + "\n" + e.getMessage());
        }
        return actionClassFound;
    }

    /**
     * Vérifie si la classe ActionForm correspondant à l'action demandée existe.
     * @param formClassFullName
     *        Nom complet de la classe de l'ActionForm correspondant à l'action
     *        demandée.
     * @return Vrai si la classe ActionForm existe, faux autrement.
     */
    private boolean doesActionFormClassExist(String formClassFullName)
    {
        boolean formClassFound = false;
        try
        {
            Class.forName(formClassFullName);
            formClassFound = true;
        }
        catch (ClassNotFoundException e)
        {
            throw new WrongActionFormCanonicalNameSpecifiedException("Whether the ActionForm class full name " + formClassFullName + " you specified in the configuration file is incorrect. Please check it. Or its class can't be loaded." + "\n" + e.getMessage());
        }
        return formClassFound;
    }
}
