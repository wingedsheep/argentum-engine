package com.wingedsheep.gymserver.dto

import com.wingedsheep.engine.gym.contract.TrainingObservation
import com.wingedsheep.engine.gym.service.EnvId
import com.wingedsheep.engine.gym.service.SnapshotHandle
import kotlinx.serialization.Serializable

/**
 * Response for `POST /envs`. Combines the new env's ID with its opening
 * observation so a Python trainer only has to round-trip once to start.
 */
@Serializable
data class CreateEnvResponse(
    val envId: EnvId,
    val observation: TrainingObservation
)

/** Body for `POST /envs/{id}/step`. */
@Serializable
data class StepBody(val actionId: Int)

/** Single entry for `POST /envs/step-batch`. */
@Serializable
data class StepBatchItem(
    val envId: EnvId,
    val actionId: Int
)

/** Result entry for `POST /envs/step-batch`. */
@Serializable
data class StepBatchResult(
    val envId: EnvId,
    val observation: TrainingObservation
)

/** Body for `POST /envs/{id}/restore`. */
@Serializable
data class RestoreBody(val handle: SnapshotHandle)

/** Body for `DELETE /envs`. List of envs to dispose. */
@Serializable
data class DisposeBody(val envIds: List<EnvId>)

/** Response for `GET /schema-hash` and `GET /health`. */
@Serializable
data class SchemaHashResponse(val schemaHash: String)

@Serializable
data class HealthResponse(val status: String = "ok")

/** Shared error envelope for `@ExceptionHandler` responses. */
@Serializable
data class ErrorResponse(
    val code: String,
    val message: String
)
