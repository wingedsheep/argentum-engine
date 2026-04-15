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
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.media.Content
import io.swagger.v3.oas.annotations.media.ExampleObject
import io.swagger.v3.oas.annotations.tags.Tag
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
@Tag(name = "Environments", description = "Create, drive and tear down MTG game environments for RL / MCTS training.")
class EnvController(
    private val multiEnvService: MultiEnvService
) {

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @Operation(
        summary = "Create a new env",
        description = """
            Reserves an `envId`, initialises a game per `EnvConfig`, and
            returns the opening observation.

            `players` must have at least two entries. Each player's `deck`
            is a discriminated union:

            - `{"type": "Explicit", "cards": {"Mountain": 17, ...}}` for a
              hand-crafted deck list keyed by card name. Variant suffixes
              like `"Plains#BLB-270"` are tolerated — the base name is
              what the engine looks up.
            - `{"type": "RandomSealed", "setCode": "BLB", "boosterCount": 8}`
              to generate a sealed deck on demand from a registered set.

            Defaults: `skipMulligans=true` (faster rollouts),
            `startingPlayerIndex=null` (random), `perspectivePlayerIndex=0`,
            `revealAll=false`. Start with an explicit deck until you're
            confident the set's basic-land variants are registered —
            sealed requires variant registration.
        """,
        requestBody = io.swagger.v3.oas.annotations.parameters.RequestBody(
            required = true,
            content = [Content(
                mediaType = "application/json",
                examples = [
                    ExampleObject(
                        name = "Explicit — mono-red Portal",
                        summary = "Constructed decks, Alice goes first, skip mulligans",
                        value = """
{
  "players": [
    {
      "name": "Alice",
      "deck": {
        "type": "Explicit",
        "cards": {
          "Mountain": 17,
          "Raging Goblin": 3
        }
      },
      "startingLife": 20
    },
    {
      "name": "Bob",
      "deck": {
        "type": "Explicit",
        "cards": {
          "Mountain": 17,
          "Raging Goblin": 3
        }
      },
      "startingLife": 20
    }
  ],
  "skipMulligans": true,
  "startingPlayerIndex": 0,
  "perspectivePlayerIndex": 0,
  "revealAll": false
}
                        """
                    ),
                    ExampleObject(
                        name = "Random sealed — Bloomburrow",
                        summary = "Both players open 8 BLB boosters and a heuristic picks the 40-card build",
                        value = """
{
  "players": [
    {
      "name": "Alice",
      "deck": { "type": "RandomSealed", "setCode": "BLB", "boosterCount": 8 }
    },
    {
      "name": "Bob",
      "deck": { "type": "RandomSealed", "setCode": "BLB", "boosterCount": 8 }
    }
  ],
  "skipMulligans": true
}
                        """
                    ),
                    ExampleObject(
                        name = "Debug — reveal all, hand-smoothed",
                        summary = "Opponent hand + libraries visible and MTGA-style hand smoothing on. Never use for real self-play.",
                        value = """
{
  "players": [
    {
      "name": "Alice",
      "deck": {
        "type": "Explicit",
        "cards": { "Mountain": 17, "Raging Goblin": 3 }
      }
    },
    {
      "name": "Bob",
      "deck": {
        "type": "Explicit",
        "cards": { "Mountain": 17, "Raging Goblin": 3 }
      }
    }
  ],
  "skipMulligans": true,
  "useHandSmoother": true,
  "revealAll": true
}
                        """
                    )
                ]
            )]
        )
    )
    @PostMapping
    fun create(@RequestBody config: EnvConfig): CreateEnvResponse {
        val created = multiEnvService.create(config)
        return CreateEnvResponse(created.envId, created.observation.observation)
    }

    @Operation(summary = "List live env IDs")
    @GetMapping
    fun list(): List<EnvId> = multiEnvService.listEnvs().toList()

    @Operation(
        summary = "Reset an existing env",
        description = "Keeps the same `envId` and reruns game initialisation with the supplied config."
    )
    @PostMapping("/{id}/reset")
    fun reset(
        @PathVariable id: String,
        @RequestBody config: EnvConfig
    ): TrainingObservation =
        multiEnvService.reset(EnvId(id), config).observation

    @Operation(
        summary = "Dispose a batch of envs",
        description = "Removes the supplied `envIds` from the registry. Idempotent — unknown IDs are silently ignored."
    )
    @DeleteMapping
    fun dispose(@RequestBody body: DisposeBody): ResponseEntity<Unit> {
        multiEnvService.dispose(body.envIds)
        return ResponseEntity.noContent().build()
    }

    // =========================================================================
    // Observations
    // =========================================================================

    @Operation(
        summary = "Observe an env without advancing",
        description = "Pass `revealAll=true` only for debug tooling — never for real self-play, since it leaks opponent hand and libraries."
    )
    @GetMapping("/{id}")
    fun observe(
        @PathVariable id: String,
        @RequestParam(required = false) revealAll: Boolean?
    ): TrainingObservation =
        multiEnvService.observe(EnvId(id), revealAll).observation

    // =========================================================================
    // Stepping
    // =========================================================================

    @Operation(
        summary = "Advance an env by one action",
        description = "`actionId` must come from the most recent observation. Stale IDs return 400."
    )
    @PostMapping("/{id}/step")
    fun step(
        @PathVariable id: String,
        @RequestBody body: StepBody
    ): TrainingObservation =
        multiEnvService.step(StepRequest(EnvId(id), body.actionId)).observation

    @Operation(
        summary = "Advance many envs in parallel",
        description = "Results are returned in request order. Safe because each env runs in its own worker thread."
    )
    @PostMapping("/step-batch")
    fun stepBatch(@RequestBody items: List<StepBatchItem>): List<StepBatchResult> {
        val requests = items.map { StepRequest(it.envId, it.actionId) }
        return multiEnvService.stepBatch(requests).map { (envId, obs) ->
            StepBatchResult(envId, obs.observation)
        }
    }

    @Operation(
        summary = "Submit a structured decision",
        description = """
            For pending decisions the folded action-ID space can't express —
            ChooseTargets, Distribute, Order, SplitPiles, Search, Reorder,
            AssignDamage, SelectManaSources, multi-select SelectCards,
            multi-mode ChooseMode, BudgetModal.
            Returns 409 if the env is not currently paused on a decision.
        """
    )
    @PostMapping("/{id}/decision")
    fun submitDecision(
        @PathVariable id: String,
        @RequestBody response: DecisionResponse
    ): TrainingObservation =
        multiEnvService.submitDecision(EnvId(id), response).observation

    // =========================================================================
    // Fork / snapshot / restore
    // =========================================================================

    @Operation(
        summary = "Fork an env N times",
        description = "Because `GameState` is immutable, each fork shares state with its source for free; divergence only costs from the first `step` onwards."
    )
    @PostMapping("/{id}/fork")
    fun fork(
        @PathVariable id: String,
        @RequestParam(defaultValue = "1") count: Int
    ): List<EnvId> =
        multiEnvService.fork(EnvId(id), count)

    @Operation(summary = "Save a snapshot of an env's current state")
    @PostMapping("/{id}/snapshot")
    fun snapshot(@PathVariable id: String): SnapshotHandle =
        multiEnvService.snapshot(EnvId(id))

    @Operation(
        summary = "Restore an env from a snapshot",
        description = "Perspective and reveal settings are preserved; only the underlying `GameState` changes."
    )
    @PostMapping("/{id}/restore")
    fun restore(
        @PathVariable id: String,
        @RequestBody body: RestoreBody
    ): TrainingObservation =
        multiEnvService.restore(EnvId(id), body.handle).observation
}
