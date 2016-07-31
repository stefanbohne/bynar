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

object Messages {
    trait Level
    case object Error extends Level
    case object Warning extends Level
    case object Info extends Level
    
    val key = new AnnotationKey[mutable.Buffer[(Level, String)]]()
    def add[T <: Annotated](thing: T, level: Level, message: String): T = {
        val list = thing.annotation(key) match {
            case None => 
                val l = mutable.Buffer[(Level, String)]()
                thing.putAnnotation(key, l)
                l
            case Some(l) => l
        }
        list += ((level, message))
        thing
    }
    def addError[T <: Annotated](thing: T, message: String): T = 
        add(thing, Error, message)
    def addWarning[T <: Annotated](thing: T, message: String): T = 
        add(thing, Warning, message)
    def addInfo[T <: Annotated](thing: T, message: String): T = 
        add(thing, Info, message)
    def get(thing: Annotated): Seq[(Level, String)] = 
        thing.annotation(key).getOrElse(Seq())
}