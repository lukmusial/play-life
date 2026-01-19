package models.com.bulba

import models.com.bulba.canvas.Random3dCanvas
import scala.collection.parallel.CollectionConverters._

class Universe[S <: Seq[Cell], T <: Seq[S]](layers: Layers[S, T])  {

  def stage(): Universe[S,T] = {
    new Universe(layers.stageStatefully())
  }

  def toNumericSequence: Seq[Seq[Seq[Long]]] = layers.par.map(_.toNumericSequence).seq

  def toHex: Seq[Seq[String]] = layers.par.map(_.toHex).seq

  override def toString : String = layers.toString()

}

object Universe {
  def apply(layersInt: Int, width: Int, height: Int) : Universe[VC, VVC] = {
    lazy val lay : Layers[VC, VVC] = new Layers[VC, VVC](for (i <- 0 until layersInt) yield new Random3dCanvas(width, height, i, lay))
    new Universe(lay)
  }
}


