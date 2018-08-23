package com.github.eastcirclek.examples.window.trigger

import com.github.eastcirclek.examples.{MyRecord, MyWatermark, StreamElement}
import com.github.eastcirclek.flink.trigger.DelayedEarlyResultEventTimeTrigger
import org.apache.flink.api.scala._
import org.apache.flink.streaming.api.TimeCharacteristic
import org.apache.flink.streaming.api.functions.source.SourceFunction.SourceContext
import org.apache.flink.streaming.api.scala.StreamExecutionEnvironment
import org.apache.flink.streaming.api.watermark.Watermark
import org.apache.flink.streaming.api.windowing.assigners.EventTimeSessionWindows
import org.apache.flink.streaming.api.windowing.time.Time._
import org.apache.flink.util.Collector

object SessionWindowWithDelayedEarlyResultTrigger {
  def main(args: Array[String]): Unit = {
    val env = StreamExecutionEnvironment.getExecutionEnvironment
    env.setParallelism(1)
    env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime)

    val records = Seq[StreamElement](
      MyRecord('a', 1),
      MyRecord('b', 3),
      MyRecord('d', 6, true),
      MyRecord('c', 5),
      MyWatermark(7),
      MyWatermark(8),
      MyWatermark(9),
      MyWatermark(10)
    )

    env
      .addSource( (context: SourceContext[MyRecord]) =>
        records foreach {
          case MyWatermark(timestamp) =>
            println(s"Generate a watermark @ $timestamp")
            context.emitWatermark(new Watermark(timestamp))
            Thread.sleep(200)
          case record@MyRecord(value, timestamp, _) =>
            println(s"$value @ $timestamp")
            context.collectWithTimestamp(record, timestamp)
            Thread.sleep(200)
        }
      )
      .windowAll(EventTimeSessionWindows.withGap(milliseconds(3)))
      .trigger(new DelayedEarlyResultEventTimeTrigger[MyRecord](_.last))
      .apply(
        (window, iterator, collector: Collector[String]) =>
          collector.collect(window.toString + " : " + iterator.mkString(", "))
      )
      .print()

    env.execute()
  }
}
