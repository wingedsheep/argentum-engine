package com.wingedsheep.engine.handlers.effects.copy

import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GrantedEntryCopyOrderContinuation
import com.wingedsheep.engine.core.EntryCopyReplay
import com.wingedsheep.engine.core.suspendForDecision
import com.wingedsheep.engine.handlers.PredicateContext
import com.wingedsheep.engine.handlers.PredicateEvaluator
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.battlefield.BattlefieldEntryTimestampComponent
import com.wingedsheep.engine.state.components.battlefield.LastKnownPermanentComponent
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.ControllerComponent
import com.wingedsheep.engine.state.components.identity.CopyOfComponent
import com.wingedsheep.engine.state.components.identity.OwnerComponent
import com.wingedsheep.engine.state.components.identity.copiableCardComponent
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EntersAsCopy
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import kotlinx.serialization.Serializable

/**
 * An entrant whose granted copies needed ordering but that was placed before its controller was
 * asked; the replay seam asks about it. [options] are [GrantedEntryCopy.Found.options].
 */
@Serializable
data class UnaskedEntryCopyOrder(
    val entityId: EntityId,
    val controllerId: EntityId,
    val entrantName: String,
    val options: List<EntryCopyChoice>,
)

/** [GameState.entryCopyOrders]: answers awaiting their replayed entrant, and the entrant to ask about. */
@Serializable
data class EntryCopyOrders(
    val answers: Map<EntityId, EntryCopyChoice> = emptyMap(),
    val unasked: UnaskedEntryCopyOrder? = null,
)

/**
 * Granted "enters as a copy of *that* object" replacements — an [EntersAsCopy] with a bound
 * [EntersAsCopy.copyOf] recorded in [GameState.grantedReplacementEffects] ("the next time one or more
 * creatures or planeswalkers enter this turn, they enter as copies of the chosen creature" — Mystic
 * Reflection). Unlike a permanent's own as-enters copy there is nothing to choose, so every entry path
 * reads it the same way: the permanent-spell resolver, the effect move pipeline (`EffectCopyEntry`,
 * `EffectEntryChoices`, the zone transition) and the token / direct-insertion paths.
 *
 * Copy effects are applied before the entrant's other entry replacements (CR 616.1c), and those
 * others are then read from the permanent as it would exist with the copy applied (CR 614.12). So
 * the copied identity is installed first, and the entrant's own "enters as a copy" choice is not
 * asked: whichever order the two copy effects applied in, it would end up a copy of the bound object.
 *
 * A [Duration.NextUse] grant is spent once it modifies an entry; it keeps applying to the objects
 * entering in that same event and is purged at the event boundary
 * ([com.wingedsheep.engine.replacement.ActiveReplacements.purgeSpent]). Every grant that applies to
 * an entry is applied to it (CR 616.1f) and so spent by it. When grants bound to different objects
 * apply to one entrant, its controller orders them (CR 616.1) and the copy effect applied last decides
 * what it copies, so the order comes down to choosing that copy: [Found.options] lists the distinct
 * copies and [Found.chosen] reads the controller's answer. A permanent spell asks as it resolves
 * ([question]). Every other entrant — an effect move, a token, a played land — is put onto the
 * battlefield with no pause before its entry, so it is asked by replay: the first entrant placed
 * without an answer is recorded as [EntryCopyOrders.unasked] (taking the most recent grant's copy
 * meanwhile), and the nearest seam — the effect executor registry, the continuation handler for a
 * resumed answer, the land play — throws that work away, asks ([askIfUnasked]) and replays it from
 * the same state with the answer in [EntryCopyOrders.answers]. Entity ids are minted from the state,
 * so the replay creates the same tokens and each finds its answer.
 *
 * A face-down entrant is still modified: it becomes a copy under its face-down status (CR 708.10) —
 * it keeps the face-down characteristics, and the copied values show if it is turned face up.
 */
object GrantedEntryCopy {

    /**
     * The granted copies that modify an entrant: every applying grant's index, and the distinct
     * copies they would make, most recently granted first. [choice] is the default, the most recent.
     */
    data class Found(val grantIndices: List<Int>, val options: List<EntryCopyChoice>) {
        val choice: EntryCopyChoice get() = options.first()

        /** Whether the entrant's controller has an order to choose: two grants copy different things. */
        val needsOrder: Boolean get() = options.size > 1

        /** The copy the controller chose to apply last ([answer], when it is one of [options]). */
        fun chosen(answer: EntryCopyChoice?): EntryCopyChoice =
            answer?.let { a -> options.firstOrNull { it.sameCopyAs(a) } } ?: choice
    }

    /** The granted copy replacements that modify [entityId]'s entry, or null when none applies. */
    fun find(state: GameState, entityId: EntityId, evaluator: PredicateEvaluator): Found? {
        val grants = state.grantedReplacementEffects
        if (grants.isEmpty()) return null
        val options = mutableListOf<EntryCopyChoice>()
        val indices = mutableListOf<Int>()
        for (index in grants.indices.reversed()) {
            val grant = grants[index]
            val replacement = grant.replacement as? EntersAsCopy ?: continue
            val referenceId = (replacement.copyOf as? EffectTarget.SpecificEntity)?.entityId ?: continue
            val pattern = replacement.appliesTo as? EventPattern.ZoneChangeEvent ?: continue
            if (pattern.to != Zone.BATTLEFIELD) continue
            val context = PredicateContext(sourceId = grant.entityId, controllerId = grant.controllerId)
            if (!evaluator.matches(state, state.projectedState, entityId, pattern.filter, context)) continue
            val copied = referenceIdentity(state, referenceId, grant.referenceSnapshot) ?: continue
            indices += index
            val option = EntryCopyChoice(replacement, copied, referenceId)
            if (options.none { it.copiedCard == copied && it.replacement.exceptions == replacement.exceptions }) {
                options += option
            }
        }
        return if (options.isEmpty()) null else Found(indices, options)
    }

    /**
     * The controller's choice of which granted copy [entrantName] ends up as — the copy effect it
     * applies last (CR 616.1). While every copied creature is still on the battlefield it is picked
     * there, like a Clone's choice; once one has left, the choice is a list of their names.
     * [chosenOption] reads the answer.
     */
    fun question(
        state: GameState,
        options: List<EntryCopyChoice>,
        entrantId: EntityId,
        entrantName: String,
        controllerId: EntityId,
    ): (String) -> com.wingedsheep.engine.core.PendingDecision {
        val prompt = "Choose what $entrantName enters as a copy of"
        // A token being replayed doesn't exist yet in the state asked from: name it only.
        val context = com.wingedsheep.engine.core.DecisionContext(
            sourceId = entrantId.takeIf { state.getEntity(it) != null }, sourceName = entrantName,
            phase = com.wingedsheep.engine.core.DecisionPhase.RESOLUTION,
        )
        val onBattlefield = options.mapNotNull { it.copiedEntity }
            .takeIf { ids -> ids.size == options.size && options.all { it.isLiveOn(state) } }
        return { decisionId ->
            if (onBattlefield != null) {
                com.wingedsheep.engine.core.SelectCardsDecision(
                    id = decisionId, playerId = controllerId, prompt = prompt, context = context,
                    options = onBattlefield, minSelections = 1, maxSelections = 1, useTargetingUI = true,
                )
            } else {
                com.wingedsheep.engine.core.ChooseOptionDecision(
                    id = decisionId, playerId = controllerId, prompt = prompt, context = context,
                    options = options.map { it.copiedCard?.name ?: "Unknown" },
                )
            }
        }
    }

    /** Whether [this] copies its object as it is now on the battlefield (the same visit, CR 400.7). */
    private fun EntryCopyChoice.isLiveOn(state: GameState): Boolean {
        val id = copiedEntity ?: return false
        return id in state.getBattlefield() && state.getEntity(id)?.copiableCardComponent() == copiedCard
    }

    /** The option a [question] answer picked, or null for an answer that isn't one. */
    fun chosenOption(
        options: List<EntryCopyChoice>,
        response: com.wingedsheep.engine.core.DecisionResponse,
    ): EntryCopyChoice? = when (response) {
        is com.wingedsheep.engine.core.OptionChosenResponse -> options.getOrNull(response.optionIndex)
        is com.wingedsheep.engine.core.CardsSelectedResponse ->
            response.selectedCards.singleOrNull()?.let { id -> options.firstOrNull { it.copiedEntity == id } }
        else -> null
    }

    /**
     * The copy [entityId] enters as: [explicit] (an effect move's prepared entry copy), else the
     * controller's answer awaiting it (consumed), else the most recent grant's — recording the
     * entrant as [EntryCopyOrders.unasked] when its controller had an order to choose.
     */
    fun decide(
        state: GameState,
        found: Found,
        entityId: EntityId,
        entrantName: String,
        controllerId: EntityId,
        explicit: EntryCopyChoice? = null,
    ): Pair<EntryCopyChoice, GameState> {
        val orders = state.entryCopyOrders
        val answer = explicit?.takeIf { e -> found.options.any { it.sameCopyAs(e) } }
            ?: orders?.answers?.get(entityId)
        var next = state
        if (orders != null && entityId in orders.answers) {
            next = next.copy(entryCopyOrders = orders.copy(answers = orders.answers - entityId))
        }
        if (answer == null && found.needsOrder && next.entryCopyOrders?.unasked == null) {
            val unasked = UnaskedEntryCopyOrder(entityId, controllerId, entrantName, found.options)
            next = next.copy(entryCopyOrders = (next.entryCopyOrders ?: EntryCopyOrders()).copy(unasked = unasked))
        }
        return found.chosen(answer) to next
    }

    /**
     * The replay seam: when running from [input] left an entrant unasked in [result], drop that
     * work and ask its controller from [input]; the answer replays it ([replay]). Null otherwise.
     */
    inline fun askIfUnasked(input: GameState, result: GameState, replay: () -> EntryCopyReplay): ExecutionResult? {
        if (result.entryCopyOrders?.unasked == null) return null
        return ask(input, result, replay())
    }

    fun ask(input: GameState, result: GameState, replay: EntryCopyReplay): ExecutionResult? {
        val unasked = result.entryCopyOrders?.unasked ?: return null
        if (input.entryCopyOrders?.unasked != null) return null
        return input.suspendForDecision(
            question(input, unasked.options, unasked.entityId, unasked.entrantName, unasked.controllerId),
            GrantedEntryCopyOrderContinuation(unasked.entityId, unasked.options, replay),
        )
    }

    /** [state] with the controller's [choice] waiting for [entityId]'s replayed entry. */
    fun withAnswer(state: GameState, entityId: EntityId, choice: EntryCopyChoice): GameState {
        val orders = state.entryCopyOrders ?: EntryCopyOrders()
        return state.copy(entryCopyOrders = orders.copy(answers = orders.answers + (entityId to choice)))
    }

    /**
     * At the end of an action: forget an unasked entrant no seam replayed (it keeps the most recent
     * grant's copy), and once nothing is pending, any answer whose entrant never came.
     */
    fun settle(state: GameState): GameState {
        val orders = state.entryCopyOrders ?: return state
        if (state.pendingDecision == null) return state.copy(entryCopyOrders = null)
        if (orders.unasked == null) return state
        return state.copy(entryCopyOrders = orders.copy(unasked = null).takeIf { it.answers.isNotEmpty() })
    }

    /** Mark every one-shot grant that modified an entry as spent. */
    fun spend(state: GameState, found: Found): GameState {
        val grants = state.grantedReplacementEffects
        val toSpend = found.grantIndices.filter { i ->
            grants.getOrNull(i)?.let { it.duration is Duration.NextUse && !it.spent } == true
        }
        if (toSpend.isEmpty()) return state
        return state.copy(grantedReplacementEffects = grants.toMutableList().also { list ->
            for (i in toSpend) list[i] = list[i].copy(spent = true)
        })
    }

    /**
     * For an entrant that is inserted onto the battlefield directly (a token, a played land): if a
     * granted copy applies, install the copy [decide] picks — its copied identity with its static and replacement payload
     * ([withCopyIdentity]) before the entrant is placed, and spend the grant.
     */
    fun applyBeforeEntry(
        state: GameState,
        entityId: EntityId,
        registry: CardRegistry,
        evaluator: PredicateEvaluator,
    ): GameState {
        val found = find(state, entityId, evaluator) ?: return state
        val entity = state.getEntity(entityId) ?: return state
        val original = entity.get<CardComponent>() ?: return state
        val controllerId = entity.get<ControllerComponent>()?.playerId
            ?: entity.get<OwnerComponent>()?.playerId ?: return state
        val (choice, decided) = decide(state, found, entityId, original.name, controllerId)
        val copied = EffectCopyEntry.copiedIdentity(original, choice) ?: return state
        val withCopy = decided.updateEntity(entityId) { c ->
            EffectCopyEntry.tagCopyDuration(
                c.withCopyIdentity(copied, registry).with(CopyOfComponent(
                    originalCardDefinitionId = original.cardDefinitionId,
                    copiedCardDefinitionId = copied.cardDefinitionId,
                    originalCardComponent = original,
                )),
                choice.replacement.duration,
            )
        }
        return spend(withCopy, found)
    }

    /**
     * The copiable values of [referenceId] as the entrant enters (the ruling: "use the copiable
     * values of the creature at the time the permanents enter"): live while the same battlefield
     * visit lasts, otherwise as it last existed on the battlefield (CR 608.2h) — the departure
     * snapshot of that visit, else the one taken when the grant was made.
     */
    private fun referenceIdentity(
        state: GameState,
        referenceId: EntityId,
        captured: com.wingedsheep.engine.state.components.stack.EntitySnapshot?,
    ): CardComponent? {
        val visit = captured?.battlefieldEntryTimestamp
        val container = state.getEntity(referenceId)
        if (container != null && referenceId in state.getBattlefield() &&
            (visit == null || container.get<BattlefieldEntryTimestampComponent>()?.timestamp == visit)
        ) {
            return container.copiableCardComponent()
        }
        val departed = container?.get<LastKnownPermanentComponent>()?.snapshot
            ?.takeIf { it.copiableCard != null && it.battlefieldEntryTimestamp == visit }
        return (departed ?: captured)?.copiableCard
    }
}
