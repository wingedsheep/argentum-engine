package com.wingedsheep.engine.handlers.effects

import com.wingedsheep.engine.core.ChooseColorDecision
import com.wingedsheep.engine.core.ChooseOptionDecision
import com.wingedsheep.engine.core.ContinuationFrame
import com.wingedsheep.engine.core.DecisionContext
import com.wingedsheep.engine.core.DecisionPhase
import com.wingedsheep.engine.core.EntersWithChoiceOnBattlefieldContinuation
import com.wingedsheep.engine.core.ExecutionResult
import com.wingedsheep.engine.core.GameEvent
import com.wingedsheep.engine.core.OptionMetadata
import com.wingedsheep.engine.core.PendingDecision
import com.wingedsheep.engine.core.SelectCardsDecision
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.PlayerComponent
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Applies a permanent's own "as-enters" [EntersWithChoice] replacement (CR 614.12 — choose a
 * color / creature type / mode / … as the permanent enters) to an entity that has *already* been
 * placed on the battlefield **directly** — i.e. not cast as a spell that resolves off the stack.
 *
 * Two callers share this seam:
 *  - [com.wingedsheep.engine.handlers.actions.land.PlayLandHandler] — a land played directly, and
 *  - [com.wingedsheep.engine.handlers.effects.token.TokenFromDefinition] — a token minted from a
 *    card definition (e.g. the Momir Basic avatar's random-creature token).
 *
 * The spell-resolution path keeps its own pre-battlefield variant
 * ([com.wingedsheep.engine.mechanics.stack.StackResolver.pauseForEntersWithChoice]) because there
 * the entity is still a `SpellOnStackComponent` on the stack, not yet a permanent.
 *
 * The choice pauses for a player decision. [EntersWithChoiceOnBattlefieldContinuation]'s resumer
 * records the chosen value into the entity's `CastChoicesComponent`, chains to any remaining
 * choice, and then fires the entry's ETB triggers off a synthesized [ZoneChangeEvent] (using the
 * continuation's `fromZone`). **ETB triggers are therefore fired by the resumer, never by the
 * caller** — a caller whose paused result is run through trigger detection (e.g. an effect
 * resolving via `StackResolver`) must NOT also include the entry battlefield [ZoneChangeEvent] in
 * [carryEvents], or the triggers fire twice.
 */
object PermanentEntryReplacements {

    /**
     * Build the paused [ExecutionResult] for a permanent's first/next [EntersWithChoice].
     *
     * @param entityId the permanent already on the battlefield.
     * @param controllerId its controller.
     * @param cardComponent its card component (for the prompt name).
     * @param choice the choice to present.
     * @param fromZone the zone the permanent came from, used to synthesize the entry
     *   [ZoneChangeEvent] in the resumer; `null` for a freshly-minted token (no prior zone).
     * @param carryEvents events already produced by the caller to forward with the pause (e.g.
     *   counters added). Must NOT include the entry battlefield [ZoneChangeEvent] when the caller's
     *   result is trigger-detected (see class docs).
     * @return a paused [ExecutionResult], or `null` if the choice cannot be presented (e.g.
     *   `CREATURE_ON_BATTLEFIELD` with no other creatures, an empty `MODE`/`OPPONENT` set) — the
     *   caller then completes entry normally.
     */
    fun pauseForEntersWithChoice(
        state: GameState,
        entityId: EntityId,
        controllerId: EntityId,
        cardComponent: CardComponent,
        choice: EntersWithChoice,
        fromZone: Zone?,
        carryEvents: List<GameEvent> = emptyList(),
        cardNameOptions: List<String> = emptyList(),
    ): ExecutionResult? {
        val chooserId = when (choice.chooser) {
            Player.AnOpponent -> state.getOpponents(controllerId).firstOrNull() ?: controllerId
            else -> controllerId
        }
        val name = cardComponent.name

        fun context(decisionId: String) = DecisionContext(
            sourceId = entityId,
            sourceName = name,
            phase = DecisionPhase.RESOLUTION
        )

        fun pause(decision: PendingDecision, continuation: ContinuationFrame): ExecutionResult {
            val paused = state.pushContinuation(continuation).withPendingDecision(decision)
            return ExecutionResult.paused(paused, decision, carryEvents)
        }

        return when (choice.choiceType) {
            ChoiceType.COLOR -> {
                val id = "choose-color-enters-${entityId.value}"
                pause(
                    ChooseColorDecision(id, chooserId, "Choose a color", context(id)),
                    EntersWithChoiceOnBattlefieldContinuation(
                        decisionId = id,
                        entityId = entityId,
                        controllerId = controllerId,
                        choiceType = ChoiceType.COLOR,
                        fromZone = fromZone
                    )
                )
            }

            ChoiceType.CREATURE_TYPE -> {
                val options = choice.allowedCreatureTypes ?: Subtype.ALL_CREATURE_TYPES
                val id = "choose-creature-type-enters-${entityId.value}"
                pause(
                    ChooseOptionDecision(
                        id = id,
                        playerId = chooserId,
                        prompt = "Choose a creature type",
                        context = context(id),
                        options = options,
                        defaultSearch = ""
                    ),
                    EntersWithChoiceOnBattlefieldContinuation(
                        decisionId = id,
                        entityId = entityId,
                        controllerId = controllerId,
                        choiceType = ChoiceType.CREATURE_TYPE,
                        creatureTypes = options,
                        fromZone = fromZone
                    )
                )
            }

            ChoiceType.CREATURE_ON_BATTLEFIELD -> {
                val creatures = state.getBattlefield().filter { eid ->
                    if (eid == entityId) return@filter false
                    val container = state.getEntity(eid) ?: return@filter false
                    val card = container.get<CardComponent>() ?: return@filter false
                    state.projectedState.getController(eid) == controllerId && card.typeLine.isCreature
                }
                if (creatures.isEmpty()) return null
                val id = "choose-creature-enters-${entityId.value}"
                pause(
                    SelectCardsDecision(
                        id = id,
                        playerId = controllerId,
                        prompt = "Choose another creature you control",
                        context = context(id),
                        options = creatures,
                        minSelections = 1,
                        maxSelections = 1,
                        useTargetingUI = true
                    ),
                    EntersWithChoiceOnBattlefieldContinuation(
                        decisionId = id,
                        entityId = entityId,
                        controllerId = controllerId,
                        choiceType = ChoiceType.CREATURE_ON_BATTLEFIELD,
                        fromZone = fromZone
                    )
                )
            }

            ChoiceType.MODE -> {
                if (choice.modeOptions.isEmpty()) return null
                val id = "choose-mode-enters-${entityId.value}"
                pause(
                    ChooseOptionDecision(
                        id = id,
                        playerId = chooserId,
                        prompt = "Choose for $name",
                        context = context(id),
                        options = choice.modeOptions.map { it.label },
                        optionMetadata = choice.modeOptions.map {
                            OptionMetadata(id = it.id, description = it.description, iconKey = it.iconKey)
                        }
                    ),
                    EntersWithChoiceOnBattlefieldContinuation(
                        decisionId = id,
                        entityId = entityId,
                        controllerId = controllerId,
                        choiceType = ChoiceType.MODE,
                        modeOptionIds = choice.modeOptions.map { it.id },
                        fromZone = fromZone
                    )
                )
            }

            ChoiceType.BASIC_LAND_TYPE -> {
                val options = Subtype.ALL_BASIC_LAND_TYPES.toList()
                val id = "choose-land-type-enters-${entityId.value}"
                pause(
                    ChooseOptionDecision(
                        id = id,
                        playerId = chooserId,
                        prompt = "Choose a basic land type",
                        context = context(id),
                        options = options,
                        defaultSearch = ""
                    ),
                    EntersWithChoiceOnBattlefieldContinuation(
                        decisionId = id,
                        entityId = entityId,
                        controllerId = controllerId,
                        choiceType = ChoiceType.BASIC_LAND_TYPE,
                        landTypes = options,
                        fromZone = fromZone
                    )
                )
            }

            ChoiceType.OPPONENT -> {
                val opponentIds = state.turnOrder.filter { it != chooserId }
                if (opponentIds.isEmpty()) return null
                val opponentNames = opponentIds.map { pid ->
                    state.getEntity(pid)?.get<PlayerComponent>()?.name ?: "Player ${pid.value}"
                }
                val id = "choose-opponent-enters-${entityId.value}"
                pause(
                    ChooseOptionDecision(
                        id = id,
                        playerId = chooserId,
                        prompt = "Choose an opponent",
                        context = context(id),
                        options = opponentNames
                    ),
                    EntersWithChoiceOnBattlefieldContinuation(
                        decisionId = id,
                        entityId = entityId,
                        controllerId = controllerId,
                        choiceType = ChoiceType.OPPONENT,
                        opponentIds = opponentIds,
                        fromZone = fromZone
                    )
                )
            }

            ChoiceType.CARD_NAME -> {
                // The option list (land card names from the registry) is supplied by the caller,
                // which has the registry in scope. If empty, there is nothing to name — complete
                // entry normally.
                val options = cardNameOptions.sorted()
                if (options.isEmpty()) return null
                val id = "choose-card-name-enters-${entityId.value}"
                pause(
                    ChooseOptionDecision(
                        id = id,
                        playerId = chooserId,
                        prompt = "Choose a land card name",
                        context = context(id),
                        options = options
                    ),
                    EntersWithChoiceOnBattlefieldContinuation(
                        decisionId = id,
                        entityId = entityId,
                        controllerId = controllerId,
                        choiceType = ChoiceType.CARD_NAME,
                        cardNames = options,
                        fromZone = fromZone
                    )
                )
            }
        }
    }
}
