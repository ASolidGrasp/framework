package com.formation.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Cette annotation @Action permettra au framework à identifier les actions et
 * leurs actionForm associés.
 * @author filippo
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface Action
{
    /**
     * Paramètre de l'annotation indiquant l'url-pattern qui déclenchera
     * l'action annotée.
     * @return
     */
    public String urlPattern();

    /**
     * Paramètre de l'annotation indiquant le petit nom de l'actionForm associé.
     * @return
     */
    public String formName();
}
