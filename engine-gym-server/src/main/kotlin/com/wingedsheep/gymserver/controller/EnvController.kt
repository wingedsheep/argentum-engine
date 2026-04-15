package com.wingedsheep.gymserver.controller

import com.wingedsheep.engine.core.DecisionResponse
import com.wingedsheep.engine.gym.contract.TrainingObservation
import com.wingedsheep.engine.gym.service.EnvConfig
import com.wingedsheep.engine.gym.service.EnvId
import com.wingedsheep.engine.gym.service.MultiEnvService
import com.wingedsheep.engine.gym.service.SnapshotHandle
import com.wingedsheep.engine.gym.service.StepRequest
import com.wingedsheep.gymserver.dto.CreateEnvResponse
import com.wingedsheep.gymserver.dto.DisposeBody
import com.wingedsheep.gymserver.dto.RestoreBody
import com.wingedsheep.gymserver.dto.StepBatchItem
import com.wingedsheep.gymserver.dto.StepBatchResult
import com.wingedsheep.gymserver.dto.StepBody
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.RestController

/**
 * HTTP façade over [MultiEnvService]. Each endpoint is a near-verbatim
 * mapping onto a service method — business logic stays in the service.
 *
 * **Concurrency model.** A trainer owns an env and calls sequentially.
 * Batched calls for different envs (`/envs/step-batch`) are safe because
 * the service fans out per-env through [com.wingedsheep.engine.gym.service.EnvWorkerPool].
 * Two concurrent calls naming the same envId race on the underlying
 * `GameEnvironment` — don't do that.
 *
 * **Action-ID stability.** IDs inside `TrainingObservation.legalActions`
 * are only valid for the observation they came in. After any step/reset,
 * the server regenerates the registry and the old IDs are invalid.
 */
@RestController
@RequestMapping("/envs")
class EnvController(
    private val multiEnvService: MultiEnvService
) {

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @PostMapping
    fun create(@RequestBody config: EnvConfig): CreateEnvResponse {
        val created = multiEnvService.create(config)
        return CreateEnvResponse(created.envId, created.observation.observation)
    }

    @GetMapping
    fun list(): List<EnvId> = multiEnvService.listEnvs().toList()

    @PostMapping("/{id}/reset")
    fun reset(
        @PathVariable id: String,
        @RequestBody config: EnvConfig
    ): TrainingObservation =
        multiEnvService.reset(EnvId(id), config).observation

    @DeleteMapping
    fun dispose(@RequestBody body: DisposeBody): ResponseEntity<Unit> {
        multiEnvService.dispose(body.envIds)
        return ResponseEntity.noContent().build()
    }

    // =========================================================================
    // Observations
    // =========================================================================

    @GetMapping("/{id}")
    fun observe(
        @PathVariable id: String,
        @RequestParam(required = false) revealAll: Boolean?
    ): TrainingObservation =
        multiEnvService.observe(EnvId(id), revealAll).observation

    // =========================================================================
    // Stepping
    // =========================================================================

    @PostMapping("/{id}/step")
    fun step(
        @PathVariable id: String,
        @RequestBody body: StepBody
    ): TrainingObservation =
        multiEnvService.step(StepRequest(EnvId(id), body.actionId)).observation

    @PostMapping("/step-batch")
    fun stepBatch(@RequestBody items: List<StepBatchItem>): List<StepBatchResult> {
        val requests = items.map { StepRequest(it.envId, it.actionId) }
        return multiEnvService.stepBatch(requests).map { (envId, obs) ->
            StepBatchResult(envId, obs.observation)
        }
    }

    @PostMapping("/{id}/decision")
    fun submitDecision(
        @PathVariable id: String,
        @RequestBody response: DecisionResponse
    ): TrainingObservation =
        multiEnvService.submitDecision(EnvId(id), response).observation

    // =========================================================================
    // Fork / snapshot / restore
    // =========================================================================

    @PostMapping("/{id}/fork")
    fun fork(
        @PathVariable id: String,
        @RequestParam(defaultValue = "1") count: Int
    ): List<EnvId> =
        multiEnvService.fork(EnvId(id), count)

    @PostMapping("/{id}/snapshot")
    fun snapshot(@PathVariable id: String): SnapshotHandle =
        multiEnvService.snapshot(EnvId(id))

    @PostMapping("/{id}/restore")
    fun restore(
        @PathVariable id: String,
        @RequestBody body: RestoreBody
    ): TrainingObservation =
        multiEnvService.restore(EnvId(id), body.handle).observation
}
