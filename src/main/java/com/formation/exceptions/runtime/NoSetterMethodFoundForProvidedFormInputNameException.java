package com.formation.exceptions.runtime;

/**
 * * Exception lancée quand aucun setter n'est trouvé dans l'actionForm pour le
 * nom d'input donné.
 * @author filippo
 */
public class NoSetterMethodFoundForProvidedFormInputNameException
        extends
        RuntimeException
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
    public NoSetterMethodFoundForProvidedFormInputNameException(String pMessage)
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
    public NoSetterMethodFoundForProvidedFormInputNameException(String pMessage, Throwable throwable)
    {
        super();
        this.message = pMessage + "\nCaused by :" + throwable.getMessage();
    }

    /**
     * Pour pouvoir chaîner les Exceptions.
     * @param throwable
     *        Exception Reprise.
     */
    public NoSetterMethodFoundForProvidedFormInputNameException(Throwable throwable)
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
