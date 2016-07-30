package org.bynar.versailles

import scala.collection._

class AnnotationKey[T]

trait Annotated { self =>
    protected val annotations = mutable.Map[AnnotationKey[_], Any]()
    
    def annotation[T](key: AnnotationKey[T]): Option[T] =
        annotations.get(key).map{ _.asInstanceOf[T] }
    def putAnnotation[T](key: AnnotationKey[T], value: T): self.type = {
        annotations += key -> value
        this
    }
    def copyAnnotationsFrom(that: Annotated): self.type = {
        this.annotations ++= that.annotations
        this
    }

}