package com.wingedsheep.ai.training

import com.wingedsheep.engine.core.GameAction
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.CountersComponent
import com.wingedsheep.engine.state.components.battlefield.TappedComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.TeamComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.security.MessageDigest

const val DECISION_TRAINING_SCHEMA_VERSION: Int = 1

/** Globally unique identity for one decision. A run id must never be reused. */
@Serializable
data class DecisionIdentity(val runId: String, val gameId: String, val decisionIndex: Int) {
    init {
        require(runId.isNotBlank() && gameId.isNotBlank())
        require(decisionIndex >= 0)
    }
}

@Serializable
data class TrainingGameMetadata(
    val runId: String,
    val gameId: String,
    val setCode: String,
    val format: String,
    val deckHashes: List<String>,
    val seed: Long,
    val profilesBySeat: List<String>,
    val teamIdsBySeat: List<Int?> = List(profilesBySeat.size) { null },
    val schemaVersion: Int = DECISION_TRAINING_SCHEMA_VERSION,
    val completionReason: String,
    val recoveredIllegalAction: Boolean = false,
    val exception: String? = null,
    val generator: String,
    /** Digest of the serialized initial state or another deterministic reconstruction payload. */
    val initialStateDigest: String = "",
    /** Ordered, polymorphic GameAction JSON. Empty only for legacy schema-v1 fixtures. */
    val actionLog: List<String> = emptyList(),
    /** Exploration probability of the chosen action, when the generator is stochastic. */
    val actionPropensities: List<Double> = emptyList(),
) {
    val globallyUniqueId: String get() = "$runId/$gameId"
    val completedCleanly: Boolean get() = completionReason == "completed" && !recoveredIllegalAction && exception == null

    init {
        require(runId.isNotBlank() && gameId.isNotBlank())
        require(setCode.isNotBlank() && format.isNotBlank() && generator.isNotBlank())
        require(deckHashes.size == profilesBySeat.size)
        require(teamIdsBySeat.size == profilesBySeat.size)
        require(actionPropensities.all { it.isFinite() && it > 0.0 && it <= 1.0 })
        require(actionPropensities.isEmpty() || actionPropensities.size == actionLog.size)
    }
}

/** A viewer-safe observation. Opponent private zones contain counts, never identities. */
@Serializable
data class MaskedObservation(
    val turnNumber: Int,
    val activePlayer: PublicPlayerRef,
    val priorityPlayer: PublicPlayerRef?,
    val self: ObservedPlayer,
    /** Canonically sorted, semantically unordered opponents. */
    val others: List<ObservedPlayer>,
    val stack: List<VisibleCard>,
) {
    init {
        require(others.size in 1..3) { "Decision records support 2-4 players." }
        require(others.none { it.ref == self.ref })
        require(others == others.sortedBy { it.ref.stableId }) { "Other players must use canonical order." }
    }
}

@Serializable
data class PublicPlayerRef(val stableId: String, val teamId: Int?)

@Serializable
data class ObservedPlayer(
    val ref: PublicPlayerRef,
    val life: Int,
    val handSize: Int,
    val librarySize: Int,
    val graveyard: List<VisibleCard>,
    val battlefield: List<VisibleCard>,
    /** Present only for the acting player. */
    val visibleHand: List<VisibleCard> = emptyList(),
)

@Serializable
data class VisibleCard(
    val stableId: String,
    val name: String,
    val zone: String,
    val controller: String? = null,
    val tapped: Boolean = false,
    val counters: Map<String, Int> = emptyMap(),
)

/** Structured engine action plus search fields reserved by the Phase 9c schema. */
@Serializable
data class CandidateTrainingRecord(
    val descriptor: CandidateDescriptor,
    val quietObservation: MaskedObservation,
    val quietStateDigest: String,
    val featureDelta: Map<String, Double> = emptyMap(),
    val staticScore: Double? = null,
    val rolloutMean: Double? = null,
    val rolloutVariance: Double? = null,
    val terminalResult: Double? = null,
    val searchAllocation: Int = 0,
    val visitCount: Int = 0,
)

@Serializable
data class CandidateDescriptor(
    val actionType: String,
    /** Polymorphic GameAction JSON. The engine, never the learner, creates this value. */
    val encodedAction: String,
    val actionDigest: String,
)

@Serializable
data class ReplayCoordinates(
    val seed: Long,
    val actionPrefixDigest: String,
    val rootQuietStateDigest: String,
)

@Serializable
data class DecisionTrainingRecord(
    val schemaVersion: Int = DECISION_TRAINING_SCHEMA_VERSION,
    val identity: DecisionIdentity,
    val format: String,
    val playerCount: Int,
    val actingSeat: Int,
    val teamIdsBySeat: List<Int?>,
    val rootObservation: MaskedObservation,
    val candidates: List<CandidateTrainingRecord>,
    val sharedSampledWorldIds: List<String> = emptyList(),
    val sharedRolloutSeeds: List<Long> = emptyList(),
    val chosenActionDigest: String? = null,
    val eventualResult: Double? = null,
    val terminalPlacementBySeat: List<Int?> = emptyList(),
    val utilityBySeat: List<Double> = emptyList(),
    val replay: ReplayCoordinates,
    /** Why a nearby root was not retained is aggregated in the run report, never as fake training rows. */
    val actionFamily: String = "unknown",
    val gamePhase: String = "unknown",
) {
    init {
        require(playerCount in 2..4)
        require(actingSeat in 0 until playerCount)
        require(teamIdsBySeat.size == playerCount)
        require(rootObservation.others.size == playerCount - 1)
        require(candidates.map { it.descriptor.actionDigest }.distinct().size == candidates.size)
        require(terminalPlacementBySeat.isEmpty() || terminalPlacementBySeat.size == playerCount)
        require(utilityBySeat.isEmpty() || utilityBySeat.size == playerCount)
        require(playerCount != 2 || utilityBySeat.isEmpty() || kotlin.math.abs(utilityBySeat.sum()) < 1e-9) {
            "Duel utility must be zero-sum [u, -u]."
        }
    }
}

object TrainingRecordEncoding {
    val json: Json = Json {
        encodeDefaults = true
        classDiscriminator = "type"
        ignoreUnknownKeys = false
    }

    fun action(action: GameAction): CandidateDescriptor {
        val encoded = json.encodeToString<GameAction>(action)
        return CandidateDescriptor(action::class.simpleName ?: "GameAction", encoded, sha256(encoded))
    }

    fun decodeAction(descriptor: CandidateDescriptor): GameAction =
        json.decodeFromString<GameAction>(descriptor.encodedAction)

    fun observation(state: GameState, viewer: EntityId): MaskedObservation {
        val refs = state.turnOrder.associateWith { id ->
            PublicPlayerRef(id.value, state.getEntity(id)?.get<TeamComponent>()?.teamIndex)
        }
        fun card(id: EntityId, zone: Zone): VisibleCard? {
            val entity = state.getEntity(id) ?: return null
            val component = entity.get<CardComponent>() ?: return null
            val controller = if (zone == Zone.BATTLEFIELD) state.projectedState.getController(id)?.value else null
            val counters = entity.get<CountersComponent>()?.counters
                ?.mapKeys { it.key.name }?.filterValues { it != 0 }?.toSortedMap().orEmpty()
            return VisibleCard(id.value, component.name, zone.name, controller, entity.has<TappedComponent>(), counters)
        }
        fun player(id: EntityId, revealHand: Boolean): ObservedPlayer = ObservedPlayer(
            ref = refs.getValue(id),
            life = state.lifeTotal(id),
            handSize = state.getHand(id).size,
            librarySize = state.getLibrary(id).size,
            graveyard = state.getGraveyard(id).mapNotNull { card(it, Zone.GRAVEYARD) }.sortedBy { it.stableId },
            battlefield = state.allBattlefieldEntities().filter {
                state.projectedState.getController(it) == id
            }.mapNotNull { card(it, Zone.BATTLEFIELD) }.sortedBy { it.stableId },
            visibleHand = if (revealHand) state.getHand(id).mapNotNull { card(it, Zone.HAND) }.sortedBy { it.stableId } else emptyList(),
        )
        return MaskedObservation(
            turnNumber = state.turnNumber,
            activePlayer = refs.getValue(requireNotNull(state.activePlayerId) { "Observation has no active player." }),
            priorityPlayer = state.priorityPlayerId?.let(refs::getValue),
            self = player(viewer, true),
            others = state.turnOrder.filter { it != viewer }.map { player(it, false) }.sortedBy { it.ref.stableId },
            stack = state.stack.mapNotNull { card(it, Zone.STACK) },
        )
    }

    fun digest(observation: MaskedObservation): String = sha256(json.encodeToString(observation))
    fun sha256(value: String): String = MessageDigest.getInstance("SHA-256")
        .digest(value.toByteArray()).joinToString("") { "%02x".format(it) }
}
