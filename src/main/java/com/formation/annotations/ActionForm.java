package com.formation.annotations;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Cette annotation @ActionForm permettra au framework à identifier les
 * actionForms.
 * @author filippo
 */
@Target(ElementType.TYPE)
@Retention(RetentionPolicy.RUNTIME)
public @interface ActionForm
{
    /**
     * Petit nom de l'ActionForm.
     * @return
     */
    public String name();
}
