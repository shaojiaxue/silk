package org.silkframework.rule.execution.local

import java.util.logging.Logger

import org.silkframework.entity.{AutoDetectValueType, Entity, EntitySchema}
import org.silkframework.execution.ExecutionReport
import org.silkframework.rule.{TransformRule, TransformSpec}
import org.silkframework.rule.execution.TransformReportBuilder
import org.silkframework.runtime.activity.ActivityContext
import org.silkframework.runtime.validation.ValidationException

import scala.util.control.NonFatal

class TransformedEntities(entities: Traversable[Entity],
                          transform: TransformSpec,
                          outputSchema: EntitySchema,
                          context: ActivityContext[ExecutionReport]) extends Traversable[Entity] {

  private val log: Logger = Logger.getLogger(this.getClass.getName)

  private val subjectRule = transform.rules.find(_.target.isEmpty)

  private val propertyRules = transform.rules.filter(_.target.nonEmpty).toIndexedSeq

  private val report = new TransformReportBuilder(propertyRules)

  private var errorFlag = false

  override def foreach[U](f: (Entity) => U): Unit = {
    // For each schema path, collect all rules that map to it
    val rulesPerPath =
      for(path <- outputSchema.typedPaths.map(_.path)) yield {
        path.propertyUri match {
          case Some(property) =>
            propertyRules.filter(_.target.get.propertyUri == property)
          case None =>
            IndexedSeq.empty
        }
      }

    var count = 0
    for(entity <- entities) {
      errorFlag = false
      val uri = subjectRule.flatMap(_(entity).headOption).getOrElse(entity.uri)
      val values =
        for(rules <- rulesPerPath) yield {
          rules.flatMap(evaluateRule(entity))
        }

      f(new Entity(uri, values, outputSchema))

      report.incrementEntityCounter()
      if(errorFlag)
        report.incrementEntityErrorCounter()

      count += 1
      if (count % 1000 == 0) {
        context.value.update(report.build())
        context.status.updateMessage(s"Executing ($count Entities)")
      }
    }

    context.value() = report.build()
  }

  private def evaluateRule(entity: Entity)(rule: TransformRule): Seq[String] = {
    try {
      rule(entity)
    } catch {
      case NonFatal(ex) =>
        log.fine("Error during execution of transform rule " + rule.name.toString + ": " + ex.getMessage)
        report.addError(rule, entity, ex)
        errorFlag = true
        Seq.empty
    }
  }
}