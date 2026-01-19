package models.com.bulba

import models.com.bulba.canvas.{DeadCanvas, Canvas}

class Layers[S <: Seq[Cell], T <: Seq[S]](var layers: Seq[Canvas[S, T]]) extends Seq[Canvas[S, T]] {
  val dead = new DeadCanvas

  def below(index : Int) : Canvas[S, T] = if (index-1<layers.length && index-1>=0) layers(index-1) else dead

  def above(index : Int) : Canvas[S, T] = if (index+1<layers.length && index+1>=0) layers(index+1) else dead

  override def apply(idx: Int): Canvas[S, T] = layers(idx)

  override def length: Int = layers.length

  override def iterator: Iterator[Canvas[S, T]] = layers.iterator

  override def toString() : String = layers map (_.toString) mkString "\n"

  def stageStatefully() : Layers[S, T] = {
    layers = layers.map(_.stage())
    this
  }
}