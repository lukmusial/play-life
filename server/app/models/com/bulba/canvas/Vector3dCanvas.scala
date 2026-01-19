package models.com.bulba.canvas

import models.com.bulba._

class Vector3dCanvas(override val canvas: Vector[Vector[Cell]],
                     index: Int,
                     layers: => Layers[Vector[Cell], Vector[Vector[Cell]]],
                     override val changedCells: Set[(Int, Int)])
  extends VectorCanvas with Finite3dCanvas[Vector[Cell], Vector[Vector[Cell]]] {


  override def canvasBelow =  layers.below(index)

  override def canvasAbove = layers.above(index)

  def stage()  = {
    val staged = stagedCells
    val changed = for (x <- staged.indices; y <- staged(x).indices; if canvas(x)(y) != staged(x)(y)) yield (x, y)
    new Vector3dCanvas(staged, index, layers, changed.toSet)
  }

  override def haveNeighborsChanged(x: Int, y: Int): Boolean = {
    haveNeighborsChangedInCanvas(x,y,canvasBelow) ||
      haveNeighborsChangedInCanvas(x,y,canvasAbove) ||
        haveNeighborsChangedInCanvas(x,y,this)
  }

  private def haveNeighborsChangedInCanvas(x: Int, y: Int, c : Canvas[Seq[Cell], Seq[Seq[Cell]]]) : Boolean =  {
    for {i1 <- x - 1 to x + 1; y1 <- y - 1 to y + 1} if (c.changedCells.contains((i1, y1))) return true
    false
  }

}