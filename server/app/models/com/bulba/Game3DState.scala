package models.com.bulba

class Game3DState[S <: Seq[Cell], T <: Seq[S]](var universe: Universe[S, T]) {

  def advance() = universe = universe.stage()

  override def toString : String = universe.toString

  def toNumericSequence : Seq[Seq[Seq[Long]]] = universe.toNumericSequence

  def toHex: Seq[Seq[String]] = universe.toHex

 }


