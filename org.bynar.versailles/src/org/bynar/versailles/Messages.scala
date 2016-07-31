package org.bynar.versailles

import scala.collection._

trait Message {
    def level: Messages.Level
}

object Messages {
    trait Level
    case object Error extends Level
    case object Warning extends Level
    case object Info extends Level
    
    val key = new AnnotationKey[mutable.Buffer[Message]]()
    def add[T <: Annotated](thing: T, message: Message): T = {
        val list = thing.annotation(key) match {
            case None => 
                val l = mutable.Buffer[Message]()
                thing.putAnnotation(key, l)
                l
            case Some(l) => l
        }
        list += message
        thing
    }
    def get(thing: Annotated): Seq[Message] = 
        thing.annotation(key).getOrElse(Seq())
}