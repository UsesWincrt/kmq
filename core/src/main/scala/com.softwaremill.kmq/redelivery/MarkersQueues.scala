package com.softwaremill.kmq.redelivery

import java.time.Clock

import com.softwaremill.kmq.{MarkerKey, MarkerValue}

class MarkersQueues(clock: Clock, messageTimeout: Long) {
  private var markersQueues = Map[Partition, MarkersQueue]()

  def handleMarker(markerOffset: Offset, k: MarkerKey, v: MarkerValue): Unit = {
    markersQueues.get(k.getPartition).foreach(_.handleMarker(markerOffset, k, v))
  }

  def smallestMarkerOffsetsPerPartition(): Map[Partition, Offset] = {
    markersQueues.flatMap { case (p, mq) =>
      mq.smallestMarkerOffset().map((p, _))
    }
  }

  def markersToRedeliver(p: Partition): List[Marker] = {
    markersQueues.get(p).map(_.markersToRedeliver()).getOrElse(Nil)
  }

  def addPartition(p: Partition, currentLastMarkerOffset: Long): Unit = {
    markersQueues += p -> new MarkersQueue(clock, messageTimeout)
  }

  def removePartition(p: Partition): Unit = {
    markersQueues -= p

  }
}