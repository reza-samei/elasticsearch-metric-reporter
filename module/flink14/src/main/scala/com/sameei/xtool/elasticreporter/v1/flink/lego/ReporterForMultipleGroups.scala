package com.sameei.xtool.elasticreporter.v1.flink.lego

import com.sameei.xtool.elasticreporter.v1.flink.lego.ReporterForMultipleGroups.{MetricData, Ref}
import org.apache.flink.metrics.{Metric, MetricGroup}
import org.apache.flink.metrics.reporter.Scheduled

import scala.collection.mutable

trait ReporterForMultipleGroups extends Open with Scheduled {

    protected val groups = mutable.HashMap.empty[String, GroupedMetrics]

    protected val removeQueue = collection.mutable.ListBuffer.empty[MetricData]

    protected def ifGroup(id: String) = groups.get(id)

    protected def theGroup(id: String): GroupedMetrics = {
        ifGroup(id).getOrElse {

            val newg = new GroupedMetrics(s"${name}.${id}")
            groups(id) = newg

            if (logger.isDebugEnabled())
                logger.debug(s"TheGroup, New: ${id}")

            newg
        }
    }

    protected def dropGroup(id: String): GroupedMetrics = groups.remove(id).get

    protected def addMetric(gid: String, key:String, name: String, metric: Metric, mg: MetricGroup) : Unit = {
        theGroup(gid).addMetric(key, name, metric, mg)
    }

    protected def dropMetric(gid: String, name: String, metric: Metric, mg: MetricGroup): Unit = {
        ifGroup(gid).map { g =>
            g.dropMetric(metric)
            if (g.isEmpty) {
                dropGroup(gid)
                if (logger.isDebugEnabled()) logger.debug(s"DropMetric, Remove Empty Group: ${g.id}")
            }
        }
    }

    override def notifyOfAddedMetric(metric : Metric, metricName : String, group : MetricGroup) : Unit = synchronized {
        select(metricName, metric, group) map { ref =>
            addMetric(ref.groupId, ref.metricKey, metricName, metric, group)
            if (logger.isDebugEnabled()) logger.debug(s"AddMetric, New, Ref: ${ref}")
        } getOrElse {
            if (logger.isDebugEnabled()) {
                val id = group.getMetricIdentifier(metricName)
                logger.debug(s"AddMetric, IGNORE, Name: ${metricName}, Metric: ${getClass.getName}, ID: ${id}")
            }
        }
    }

    override def notifyOfRemovedMetric(metric : Metric, metricName : String, group : MetricGroup) : Unit = synchronized {
        select(metricName, metric, group).map { ref =>
            removeQueue :+ MetricData(ref, metricName, metric, group)
            if (logger.isDebugEnabled()) logger.debug(s"DropMetric, Queue += ${ref}")
        } getOrElse {
            if (logger.isDebugEnabled()) {
                val id = group.getMetricIdentifier(metricName)
                logger.debug(s"DropMetric, IGNORE, Name: ${metricName}, Metric: ${getClass.getName}, ID: ${id}")
            }
        }
    }

    override def report() : Unit = {

        val startedAt = System.currentTimeMillis()

        reporter.applyAll(groups.values.toSeq, startedAt)

        val endedAt = System.currentTimeMillis

        if (logger.isDebugEnabled()) logger.debug(s"Report, Time: ${startedAt}, Taked Millis: ${endedAt - startedAt}, Groups: ${groups.size}")

        if (removeQueue.nonEmpty) synchronized {
            val size = removeQueue.size
            removeQueue.foreach { data =>
                dropMetric(data.ref.groupId, data.name, data.value, data.group)
            }
            removeQueue.clear()
            if (logger.isDebugEnabled()) logger.debug(s"Report, Drop Metric, Clear Queue: ${size}")
        }
    }

    protected def select(name: String, metric: Metric, group: MetricGroup): Option[Ref]
}


object ReporterForMultipleGroups {

    case class Ref(groupId: String, metricKey: String)

    case class MetricData(ref: Ref, name: String, value: Metric, group: MetricGroup)

}