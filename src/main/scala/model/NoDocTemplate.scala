package org.scalex
package model

/**
 * A template (class, trait, object or package) which is referenced in the universe, but for which no further
 * documentation is available. Only templates for which a source file is given are documented by Scaladoc.
 */
case class NoDocTemplate(

    /** NoDocTemplate is a Template */
    template: Template) {
}
