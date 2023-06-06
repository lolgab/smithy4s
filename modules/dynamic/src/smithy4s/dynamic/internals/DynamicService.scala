/*
 *  Copyright 2021-2022 Disney Streaming
 *
 *  Licensed under the Tomorrow Open Source Technology License, Version 1.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *     https://disneystreaming.github.io/TOST-1.0.txt
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */

package smithy4s
package dynamic
package internals

import smithy4s.kinds.PolyFunction5

// TODO: better name
private[internals] class StDynamicService(
    override val service: DynamicService
) extends StaticService[PolyFunction5.From[StaticOp]#Algebra] {

  type Alg[F[_, _, _, _, _]] = PolyFunction5[DynamicOp, F]

  override def mapK5[F[_, _, _, _, _], G[_, _, _, _, _]](
      alg: PolyFunction5[StaticOp, F],
      function: PolyFunction5[F, G]
  ): PolyFunction5[StaticOp, G] =
    alg.andThen(function)

  override def endpoints: PolyFunction5[StaticOp, service.Endpoint] =
    new PolyFunction5[StaticOp, service.Endpoint] {
      override def apply[I, E, O, SI, SO](
          op: StaticOp[I, E, O, SI, SO]
      ): smithy4s.Endpoint[DynamicOp, I, E, O, SI, SO] = {
        ???
      }
    }

  override def toPolyFunction[P2[_, _, _, _, _]](
      algebra: PolyFunction5[StaticOp, P2]
  ): PolyFunction5[service.Endpoint, P2] =
    new PolyFunction5[service.Endpoint, P2] {
      override def apply[I, E, O, SI, SO](
          endpoint: smithy4s.Endpoint[DynamicOp, I, E, O, SI, SO]
      ): P2[I, E, O, SI, SO] = {
        algebra.apply(StaticOp(endpoint.id))
      }
    }

}

private[internals] case class DynamicService(
    id: ShapeId,
    version: String,
    endpoints: List[DynamicEndpoint],
    hints: Hints
) extends Service.Reflective[DynamicOp]
    with DynamicSchemaIndex.ServiceWrapper {

  type StaticAlg[P[_, _, _, _, _]] = PolyFunction5[StaticOp, P]
  override val static: StaticService.Aux[StaticAlg, Alg] = new StDynamicService(
    this
  )

  type Alg[P[_, _, _, _, _]] = PolyFunction5.From[DynamicOp]#Algebra[P]
  override val service: Service[Alg] = this

  private lazy val endpointMap: Map[ShapeId, Endpoint[_, _, _, _, _]] =
    endpoints.map(ep => ep.id -> ep).toMap

  def endpoint[I, E, O, SI, SO](
      op: DynamicOp[I, E, O, SI, SO]
  ): (I, Endpoint[I, E, O, SI, SO]) = {
    val endpoint = endpointMap
      .getOrElse(op.id, sys.error("Unknown endpoint: " + op.id))
      .asInstanceOf[Endpoint[I, E, O, SI, SO]]
    val input = op.data
    (input, endpoint)
  }

}

object DynamicService {}
