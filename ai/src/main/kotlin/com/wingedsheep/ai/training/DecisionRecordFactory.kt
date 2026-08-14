package com.wingedsheep.ai.training

import com.wingedsheep.ai.engine.GameSimulator
import com.wingedsheep.ai.engine.SimulationResult
import com.wingedsheep.engine.legalactions.EnumerationMode
import com.wingedsheep.engine.legalactions.LegalActionEnumerator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.TeamComponent
import com.wingedsheep.sdk.model.EntityId

/** Optional offline collector. Production AI does not depend on or allocate this object. */
class DecisionRecordFactory(registry: CardRegistry) {
    private val enumerator = LegalActionEnumerator.create(registry)
    private val simulator = GameSimulator(registry)

    fun capture(
        state: GameState,
        actingPlayer: EntityId,
        identity: DecisionIdentity,
        format: String,
        gameSeed: Long,
        teamIdsBySeat: List<Int?> = state.turnOrder.map {
            state.getEntity(it)?.get<TeamComponent>()?.teamIndex
        },
        actionPrefixDigest: String,
        sampledWorldIds: List<String> = emptyList(),
        rolloutSeeds: List<Long> = emptyList(),
        requireMeaningful: Boolean = false,
    ): DecisionTrainingRecord {
        require(state.pendingDecision == null) { "Pending decisions require structured response enumeration." }
        val root = TrainingRecordEncoding.observation(state, actingPlayer)
        val enumerated = enumerator.enumerate(state, actingPlayer, EnumerationMode.ACTIONS_ONLY)
        val legal = MeaningfulDecisionRoots.classify(enumerated)
        require(!requireMeaningful || legal.shouldCapture) { "Decision root is not meaningful: ${legal.skipReason}" }
        val retained = if (requireMeaningful) legal.candidates else enumerated
        val candidates = retained.map { legalAction ->
            val descriptor = TrainingRecordEncoding.action(legalAction.action)
            val simulation = simulator.simulate(state, legalAction.action)
            require(simulation is SimulationResult.Terminal) {
                "Candidate is not fully specified or legal: ${descriptor.actionType} (${simulation::class.simpleName})"
            }
            val quiet = simulation.state
            val observation = TrainingRecordEncoding.observation(quiet, actingPlayer)
            CandidateTrainingRecord(descriptor, observation, TrainingRecordEncoding.digest(observation))
        }
        return DecisionTrainingRecord(
            identity = identity,
            format = format,
            playerCount = state.turnOrder.size,
            actingSeat = state.turnOrder.indexOf(actingPlayer),
            teamIdsBySeat = teamIdsBySeat,
            rootObservation = root,
            candidates = candidates,
            sharedSampledWorldIds = sampledWorldIds,
            sharedRolloutSeeds = rolloutSeeds,
            replay = ReplayCoordinates(
                seed = gameSeed,
                actionPrefixDigest = actionPrefixDigest,
                rootQuietStateDigest = TrainingRecordEncoding.digest(root),
            ),
            actionFamily = candidates.map { it.descriptor.actionType }.distinct().sorted().joinToString("+"),
            gamePhase = state.step.name,
        )
    }
}

data class ReplayValidation(val valid: Boolean, val errors: List<String>)

/** Replays a record against a root reconstructed by the corpus owner from its game manifest. */
class DecisionRecordReplayer(registry: CardRegistry) {
    private val enumerator = LegalActionEnumerator.create(registry)
    private val simulator = GameSimulator(registry)

    fun validate(record: DecisionTrainingRecord, reconstructedRoot: GameState): ReplayValidation {
        val player = reconstructedRoot.turnOrder.getOrNull(record.actingSeat)
            ?: return ReplayValidation(false, listOf("acting seat is absent"))
        val errors = mutableListOf<String>()
        val root = TrainingRecordEncoding.observation(reconstructedRoot, player)
        if (TrainingRecordEncoding.digest(root) != record.replay.rootQuietStateDigest) errors += "root digest mismatch"
        val emitted = enumerator.enumerate(reconstructedRoot, player, EnumerationMode.ACTIONS_ONLY)
            .associateBy { TrainingRecordEncoding.action(it.action).actionDigest }
        if (emitted.keys != record.candidates.map { it.descriptor.actionDigest }.toSet()) errors += "legal candidates differ"
        for (candidate in record.candidates) {
            val legal = emitted[candidate.descriptor.actionDigest] ?: continue
            val quiet = simulator.simulate(reconstructedRoot, legal.action).state
            val digest = TrainingRecordEncoding.digest(TrainingRecordEncoding.observation(quiet, player))
            if (digest != candidate.quietStateDigest) errors += "quiet digest mismatch: ${candidate.descriptor.actionDigest}"
        }
        return ReplayValidation(errors.isEmpty(), errors)
    }
}
