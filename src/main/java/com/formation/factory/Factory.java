package com.formation.factory;

import org.apache.log4j.Logger;

/**
 * Classe où sont regroupées toutes les instanciations d'Action et ActionForm.
 * En général la factory permet de centraliser l'instanciation d'objets pour
 * éviter que des new trainent un peu partout. Des news qui trainent par ci par
 * là sont autant de dépendances à changer si l'on change d'implémentation.
 * @author filippo
 */
public final class Factory
{
    /**
     * Logger.
     */
    private static Logger logger = Logger.getLogger(Factory.class);
    /**
     * La factory est créée en singleton et il s'agit là de l'instance délivrée
     * par getFactory.
     */
    private static Factory factoryInstance = new Factory();

    /**
     * Constructeur de la factory qui est private pour que l'instanciation ne
     * soit pas accessible depuis l'extérieur. On veut pouvoir contrôler
     * l'instanciation de l'intérieur. Attention à ne pas supprimer ce
     * constructeur autrement il devient package.
     */
    private Factory()
    {
    }

    /**
     * Seule méthode publique permettant de récupérer une instance de la
     * Factory.
     * @return l'instance de la Factory.
     */
    public static Factory getFactory()
    {
        return factoryInstance;
    }

    /**
     * C'est le coeur de la fabrique, là òu tous les petits Action et ActionForm
     * sont créés avec soin. Cette fabrique est d'ailleurs capable de créer
     * absolument tout. ;)
     * @param classPath
     *        Le nom canonique de la classe de l'objet à instancier.
     * @param <T>
     *        Le type de la référence retournée.
     * @return La référence à l'instance créé non upcastée (type de variable =
     *         type d'objet référencé)
     * @throws ClassNotFoundException
     *         Retournée si l'argument passé, étant mal écrit, ne correspond pas
     *         à une classe.
     */
    @SuppressWarnings("unchecked")
    public static <T> T getInstance(String classPath) throws ClassNotFoundException
    {
        Class type;
        T result = null;

        type = Class.forName(classPath);
        try
        {
            result = (T) type.newInstance();
        }
        catch (InstantiationException | IllegalAccessException e)
        {
            logger.error("For un unknown reason the specified class can't be accessed for instanciation");
        }
        return result;
    }
}
