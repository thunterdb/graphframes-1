/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.graphframes.lib

import java.util

import scala.reflect.runtime.universe._
import scala.collection.JavaConversions._

import org.apache.spark.graphx.{lib => graphxlib}

import org.graphframes.GraphFrame

/**
 * Computes shortest paths to the given set of landmark vertices, returning a graph where each
 * vertex attribute is a map containing the shortest-path distance to each reachable landmark.
 */
object ShortestPaths {

  /**
   * Computes shortest paths to the given set of landmark vertices.
   *
   * @param graph the graph for which to compute the shortest paths
   * @param landmarks the list of landmark vertex ids. Shortest paths will be computed to each
   * landmark.
   * @return a graph where each vertex attribute is a map containing the shortest-path distance to
   * each reachable landmark vertex.
   */
  def run[VertexId: TypeTag](graph: GraphFrame, landmarks: Seq[VertexId]): GraphFrame = {
    val longLandmarks = landmarks.map(PageRank.integralId(graph, _))
    val gx = graphxlib.ShortestPaths.run(
      graph.cachedTopologyGraphX,
      longLandmarks).mapVertices { case (_, m) => m.toSeq }
    GraphXConversions.fromGraphX(graph, gx, vertexNames = Seq(DISTANCE_ID))
  }

  private val DISTANCE_ID = "distance"

  class Builder private[graphframes] (graph: GraphFrame) extends Arguments {
    private var lmarks: Option[Seq[Any]] = None

    def setLandmarks[VertexType](landmarks: Seq[VertexType]): this.type = {
      // TODO(tjh) do some initial checks here, without running queries.
      lmarks = Some(landmarks)
      this
    }

    def setLandmarks[VertexType](landmarks: util.ArrayList[VertexType]): this.type = {
      setLandmarks(landmarks.toSeq)
    }

    def run(): GraphFrame = {
      ShortestPaths.run(graph, check(lmarks, "landmarks"))
    }
  }
}