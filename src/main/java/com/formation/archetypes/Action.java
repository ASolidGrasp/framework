package com.formation.archetypes;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * Interface dont vont hériter les Actions des utilisateurs et qui impose la
 * définition d'une méthode execute qui gèrera les règles de navigation.
 * @author filippo
 */
public interface Action
{
    /**
     * Process the specified HTTP request, and create the corresponding HTTP
     * response (or forward to another web component that will create it), with
     * provision for handling exceptions thrown by the business logic. Return an
     * ActionForward instance describing where and how control should be
     * forwarded, or null if the response has already been completed.
     * @param request
     * @param response
     * @return
     * @throws Exception
     */
    /**
     * Méthode qui gère les règles de navigation.
     * @param request La requête HTTP reçue.
     * @param response La réponse HTTP qui sera renvoyée.
     * @return La resource à atteindre suite à cette action.
     */
    public String execute(HttpServletRequest request, HttpServletResponse response);

}
