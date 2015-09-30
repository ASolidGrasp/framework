package com.formation.archetypes;

import javax.servlet.http.HttpServletRequest;
/**
 * Classe abstraite dont vont hériter les ActionForm des utilisateurs et qui propose des méthodes pour les classes dérivées.
 * <ul>
 * <li>validate : retourne true si et seulement si certaines conditions définies sur les champs du formulaire sont vérifiées.</li>
 * <li>reset : méthode servant à initialiser l'ActionForm.</li>
 * </ul>
 * @author filippo
 *
 */
public abstract class ActionForm implements java.io.Serializable
{
    /**
     * Validate the properties that have been set for this HTTP request, and
     * return a boolean that signals any validation errors that have been found.
     * If no errors are found, return true. The default implementation performs
     * no validation and returns false. Subclasses must override this method to
     * provide any validation they wish to perform.
     * @param request La requête HTTP reçue.
     * @return Vrai si les conditions définies sur les champs du formulaire soumis sont remplies.
     */
    public boolean validate(HttpServletRequest request)
    {
        return false;
    }

    /**
     * Reset bean properties to their default state, as needed. This method is
     * called before the properties are repopulated by the controller. The
     * default implementation does nothing. In practice, the only properties
     * that need to be reset are those which represent checkboxes on a
     * session-scoped form. Otherwise, properties can be given initial values
     * where the field is declared. If the form is stored in session-scope so
     * that values can be collected over multiple requests (a "wizard"), you
     * must be very careful of which properties, if any, are reset. As
     * mentioned, session-scope checkboxes must be reset to false for any page
     * where this property is set. This is because the client does not submit a
     * checkbox value when it is clear (false). If a session-scoped checkbox is
     * not proactively reset, it can never be set to false. This method is not
     * the appropriate place to initialize form value for an "update" type page
     * (this should be done in a setup Action). You mainly need to worry about
     * setting checkbox values to false; most of the time you can leave this
     * method unimplemented.
     * @param request La requête HTTP reçue.
     */
    public void reset(HttpServletRequest request)
    {
        return;
    }
}
