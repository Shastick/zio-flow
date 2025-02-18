/*
 * Copyright 2021-2022 John A. De Goes and the ZIO Contributors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package zio.flow

import zio.flow.serialization.FlowSchemaAst
import zio.schema._

sealed trait Operation[-R, +A] {
  val inputSchema: Schema[_ >: R]
  val resultSchema: Schema[_ <: A]
}

object Operation {
  final case class Http[R, A](
    url: java.net.URI,
    method: String = "GET",
    headers: Map[String, String],
    inputSchema: Schema[R],
    resultSchema: Schema[A]
  ) extends Operation[R, A]

  object Http {
    def schema[R, A]: Schema[Http[R, A]] =
      Schema.CaseClass5[java.net.URI, String, Map[String, String], FlowSchemaAst, FlowSchemaAst, Http[R, A]](
        Schema.Field("url", Schema[java.net.URI]),
        Schema.Field("method", Schema[String]),
        Schema.Field("headers", Schema.map[String, String]),
        Schema.Field("inputSchema", FlowSchemaAst.schema),
        Schema.Field("outputSchema", FlowSchemaAst.schema),
        { case (url, method, headers, inputSchemaAst, outputSchemaAst) =>
          Http(
            url,
            method,
            headers,
            inputSchemaAst.toSchema[R],
            outputSchemaAst.toSchema[A]
          )
        },
        _.url,
        _.method,
        _.headers,
        op => FlowSchemaAst.fromSchema(op.inputSchema),
        op => FlowSchemaAst.fromSchema(op.resultSchema)
      )

    def schemaCase[R, A]: Schema.Case[Http[R, A], Operation[R, A]] =
      Schema.Case("Http", schema[R, A], _.asInstanceOf[Http[R, A]])
  }

  final case class SendEmail(
    server: String,
    port: Int
  ) extends Operation[EmailRequest, Unit] {

    override val inputSchema  = Schema[EmailRequest]
    override val resultSchema = Schema[Unit]
  }

  object SendEmail {
    val schema: Schema[SendEmail] = DeriveSchema.gen

    def schemaCase[R, A]: Schema.Case[SendEmail, Operation[R, A]] =
      Schema.Case("SendEmail", schema, _.asInstanceOf[SendEmail])
  }

  implicit def schema[R, A]: Schema[Operation[R, A]] =
    Schema.EnumN(
      CaseSet
        .Cons(Http.schemaCase[R, A], CaseSet.Empty[Operation[R, A]]())
        .:+:(SendEmail.schemaCase[R, A])
    )
}

final case class EmailRequest(
  to: List[String],
  from: Option[String],
  cc: List[String],
  bcc: List[String],
  body: String
)

object EmailRequest {
  implicit val emailRequestSchema: Schema[EmailRequest] = DeriveSchema.gen[EmailRequest]
}
