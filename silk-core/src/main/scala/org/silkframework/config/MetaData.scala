package org.silkframework.config

import java.time.Instant
import org.silkframework.runtime.serialization.{ReadContext, WriteContext, XmlFormat}
import scala.xml._

/**
  * Holds meta data about a task.
  */
case class MetaData(label: String, description: String, modified: Option[Instant] = None) {

  /**
    * Returns the label if defined or a default string if the label is empty. Truncates the label to maxLength characters.
    *
    * @param defaultLabel A default label that should be returned if the label is empty
    * @param maxLength the max length in characters
    */
  def formattedLabel(defaultLabel: String, maxLength: Int = MetaData.DEFAULT_LABEL_MAX_LENGTH): String = {
    assert(maxLength > 5, "maxLength for task label must be at least 5 chars long")
    val trimedLabel = if(label.trim != "") {
      label.trim
    } else {
      defaultLabel
    }
    if(trimedLabel.length > maxLength) {
      val sideLength = (maxLength - 2) / 2
      trimedLabel.take(sideLength) + s" ... " + trimedLabel.takeRight(sideLength)
    } else {
      trimedLabel
    }
  }

}

object MetaData {

  val DEFAULT_LABEL_MAX_LENGTH = 50

  def empty: MetaData = MetaData("", "")

  /**
    * XML serialization format.
    */
  implicit object MetaDataXmlFormat extends XmlFormat[MetaData] {
    /**
      * Deserialize a value from XML.
      */
    def read(node: Node)(implicit readContext: ReadContext): MetaData = {
      MetaData(
        label = (node \ "Label").text,
        description = (node \ "Description").text,
        modified = (node \ "Modified").headOption.map(node => Instant.parse(node.text))
      )
    }

    /**
      * Serialize a value to XML.
      */
    def write(data: MetaData)(implicit writeContext: WriteContext[Node]): Node = {
      <MetaData>
        <Label>{data.label}</Label>
        <Description>{data.description}</Description>
        { data.modified.map(instant => <Modified>{instant.toString}</Modified>).toSeq }
      </MetaData>
    }
  }

}