package org.silkframework.plugins.dataset.rdf

import com.hp.hpl.jena.query.QueryFactory
import org.silkframework.config.CustomTask
import org.silkframework.dataset.rdf.SparqlEndpointEntitySchema
import org.silkframework.entity.{AutoDetectValueType, EntitySchema, Path, TypedPath}
import org.silkframework.runtime.plugin.{Param, Plugin}
import org.silkframework.runtime.validation.ValidationException
import org.silkframework.util.Uri

import scala.collection.JavaConverters._
import scala.util.Try

/**
  * Custom task that executes a SPARQL select query on the input data source and translates the SPARQL result into
  * an Entity table.
  */
@Plugin(
  id = "sparqlSelectOperator",
  label = "SPARQL Select Task",
  description = "A task that executes a SPARQL Select query on a SPARQL enabled data source and outputs the SPARQL result."
)
case class SparqlSelectCustomTask(@Param(label = "Select query", value = "A SPARQL 1.1 select query", example = "select * where { ?s ?p ?o }")
                                  selectQuery: String,
                                  @Param(label = "Result limit", value = "If set to a positive integer, the number of results is limited")
                                  limit: String = "") extends CustomTask {
  val intLimit: Option[Int] = {
    // Only allow positive ints
    Try(limit.toInt).filter(_ > 0).toOption
  }

  override def inputSchemataOpt: Option[Seq[EntitySchema]] = Some(Seq(SparqlEndpointEntitySchema.schema))

  override def outputSchemaOpt: Option[EntitySchema] = Some(outputSchema)

  val outputSchema: EntitySchema = {
    val query = QueryFactory.create(selectQuery)
    if (!query.isSelectType) {
      throw new ValidationException("Query is not a SELECT query!")
    }
    val typedPaths = query.getResultVars.asScala map { v =>
      TypedPath(Path(v), AutoDetectValueType)
    }
    EntitySchema(
      typeUri = Uri(""),
      typedPaths = typedPaths.toIndexedSeq
    )
  }
}
