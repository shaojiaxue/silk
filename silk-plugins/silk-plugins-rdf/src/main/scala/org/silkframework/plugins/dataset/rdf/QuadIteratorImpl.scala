package org.silkframework.plugins.dataset.rdf

import org.silkframework.dataset.DataSource
import org.silkframework.dataset.rdf.{Quad, QuadFormatter, QuadIterator, TripleIterator}
import org.silkframework.entity.Entity

/**
  * Abstracts the quad interface of a construct query result as Iterator
  * @param hasQuad - indicating the existence of another quad
  * @param nextQuad - provisions the next quad
  */
class QuadIteratorImpl(
   val hasQuad: () => Boolean,
   val nextQuad: () => Quad,
   val close: () => Unit = () => Unit,
   val formatter: QuadFormatter
 ) extends QuadIterator {


  def asTriples: TripleIterator = new TripleIteratorImpl(hasQuad, () => nextQuad().toTriple, close, formatter)

  def asEntities: Traversable[Entity] = {
    var count = 0L
    this.toTraversable.map( quad => {
      count += 1
      quad.toEntity(Some(DataSource.URN_NID_PREFIX + count))
    })
  }
}
