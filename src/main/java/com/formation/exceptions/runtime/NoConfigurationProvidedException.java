package com.formation.exceptions.runtime;

/**
 * Exception lancée quand aucune configuration n'a été faite par l'utilisateur.
 * Pas de fichier de configuration, ni d'annotations.
 * @author filippo
 */
public class NoConfigurationProvidedException extends RuntimeException
{
    /**
     * Le message de l'exception.
     */
    private String message;

    /**
     * Constructeur le plus simple.
     * @param pMessage
     *        Le message de l'exception.
     */
    public NoConfigurationProvidedException(String pMessage)
    {
        super();
        this.message = pMessage;
    }

    /**
     * Pour pouvoir chaîner les Exceptions.
     * @param pMessage
     *        Le message de l'exception.
     * @param throwable
     *        Exception Reprise.
     */
    public NoConfigurationProvidedException(String pMessage, Throwable throwable)
    {
        super();
        this.message = pMessage + "\nCaused by :" + throwable.getMessage();
    }

    /**
     * Pour pouvoir chaîner les Exceptions.
     * @param throwable
     *        Exception Reprise.
     */
    public NoConfigurationProvidedException(Throwable throwable)
    {
        super();
        this.message = throwable.getMessage();
    }

    /**
     * Getter du message.
     * @return Le message de l'Exception
     */
    public String getMessage()
    {
        return message;
    }

    /**
     * Setter du message.
     * @param pMessage
     *        Le message de l'Exception
     */
    public void setMessage(String pMessage)
    {
        this.message = pMessage;
    }

}
