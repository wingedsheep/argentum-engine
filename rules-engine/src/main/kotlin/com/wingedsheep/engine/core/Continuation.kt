package com.wingedsheep.engine.core

import com.wingedsheep.engine.event.PendingTrigger
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.costs.PayCost
import com.wingedsheep.sdk.scripting.effects.ChainCopyEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.targets.TargetRequirement
import kotlinx.serialization.Serializable

/**
 * Represents a continuation frame - a reified "what to do next" after a decision.
 *
 * When the engine pauses for player input (e.g., "choose cards to discard"),
 * it pushes a ContinuationFrame onto the stack describing how to resume
 * execution once the player responds.
 *
 * This is a serializable alternative to closures/lambdas, allowing the
 * continuation state to be persisted and transferred across sessions.
 */
@Serializable
sealed interface ContinuationFrame {
    /** The decision ID this continuation is waiting for */
    val decisionId: String
}

/**
 * Resume after player selects cards to discard.
 *
 * @property playerId The player who is discarding
 * @property sourceId The spell/ability that caused the discard (for events)
 * @property sourceName Name of the source for event messages
 * @property controllerId If non-null, the controller who draws after the discard (for Syphon Mind)
 * @property controllerDrawsPerDiscard Cards the controller draws per card discarded (0 = no draw)
 */
@Serializable
data class DiscardContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val controllerId: EntityId? = null,
    val controllerDrawsPerDiscard: Int = 0
) : ContinuationFrame

/**
 * Resume a composite effect with remaining effects to execute.
 *
 * When a sub-effect of a CompositeEffect pauses for a decision, we push
 * this frame to remember which effects still need to run after the
 * decision is resolved.
 *
 * @property remainingEffects Effects that still need to execute (serialized)
 * @property context The execution context for these effects
 */
@Serializable
data class EffectContinuation(
    override val decisionId: String,
    val remainingEffects: List<Effect>,
    val sourceId: EntityId?,
    val controllerId: EntityId,
    val opponentId: EntityId?,
    val xValue: Int?,
    val targets: List<ChosenTarget> = emptyList(),
    val storedCollections: Map<String, List<EntityId>> = emptyMap(),
    val chosenCreatureType: String? = null,
    val triggeringEntityId: EntityId? = null,
    val namedTargets: Map<String, ChosenTarget> = emptyMap(),
    val chosenValues: Map<String, String> = emptyMap(),
    val storedNumbers: Map<String, Int> = emptyMap(),
    val storedStringLists: Map<String, List<String>> = emptyMap()
) : ContinuationFrame {
    /**
     * Reconstruct the EffectContext from serialized fields.
     */
    fun toEffectContext(): EffectContext = EffectContext(
        sourceId = sourceId,
        controllerId = controllerId,
        opponentId = opponentId,
        xValue = xValue,
        targets = targets,
        storedCollections = storedCollections,
        chosenCreatureType = chosenCreatureType,
        triggeringEntityId = triggeringEntityId,
        namedTargets = namedTargets,
        chosenValues = chosenValues,
        storedNumbers = storedNumbers,
        storedStringLists = storedStringLists
    )
}

/**
 * Resume placing a triggered ability on the stack after targets have been selected.
 *
 * When a triggered ability requires targets (like Fire Imp's "deal 1 damage to any target"),
 * we cannot put it directly on the stack. Instead, we pause to ask the player for targets,
 * storing this continuation to remember which ability we're processing.
 *
 * @property sourceId The permanent that has the triggered ability
 * @property sourceName Name of the source card for display
 * @property controllerId The player who controls the triggered ability
 * @property effect The effect to execute when the ability resolves
 * @property description Human-readable description of the ability
 */
@Serializable
data class TriggeredAbilityContinuation(
    override val decisionId: String,
    val sourceId: EntityId,
    val sourceName: String,
    val controllerId: EntityId,
    val effect: Effect,
    val description: String,
    val triggerDamageAmount: Int? = null,
    val triggeringEntityId: EntityId? = null,
    val elseEffect: Effect? = null,
    val targetRequirements: List<TargetRequirement> = emptyList()
) : ContinuationFrame

/**
 * Stores remaining pending triggers that still need to be processed.
 *
 * When multiple triggered abilities fire from the same event and the first
 * requires target selection (pausing execution), the remaining triggers are
 * stored in this continuation frame. After the first trigger's targets are
 * selected, the remaining triggers are processed.
 */
@Serializable
data class PendingTriggersContinuation(
    override val decisionId: String,
    val remainingTriggers: List<PendingTrigger>
) : ContinuationFrame

/**
 * Resume placing a triggered ability on the stack after the player answers a "may" question.
 *
 * When a triggered ability has both a MayEffect wrapper and targets (like Invigorating Boon's
 * "you may put a +1/+1 counter on target creature"), the may question is asked FIRST.
 * If the player says yes, we then proceed to target selection.
 * If the player says no, the trigger is skipped entirely.
 *
 * @property trigger The full pending trigger to process if the player says yes
 * @property targetRequirement The target requirement for the ability
 */
@Serializable
data class MayTriggerContinuation(
    override val decisionId: String,
    val trigger: PendingTrigger,
    val targetRequirement: TargetRequirement
) : ContinuationFrame

/**
 * Resume putting a card from hand onto the battlefield after card selection.
 *
 * @property playerId The player selecting a card from their hand
 * @property entersTapped Whether the card enters the battlefield tapped
 * @property sourceId The source entity that triggered this effect
 * @property sourceName Name of the source for display
 */
@Serializable
data class PutFromHandContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val entersTapped: Boolean,
    val sourceId: EntityId?,
    val sourceName: String?
) : ContinuationFrame

/**
 * Resume combat damage assignment for creatures with DivideCombatDamageFreely.
 *
 * @property attackerId The attacking creature assigning damage
 * @property defendingPlayerId The defending player
 * @property firstStrike Whether this is during the first strike combat damage step
 */
@Serializable
data class DamageAssignmentContinuation(
    override val decisionId: String,
    val attackerId: EntityId,
    val defendingPlayerId: EntityId,
    val firstStrike: Boolean = false
) : ContinuationFrame

/**
 * Resume spell resolution after target or mode selection.
 *
 * @property spellId The spell entity on the stack
 * @property casterId The player who cast the spell
 */
@Serializable
data class ResolveSpellContinuation(
    override val decisionId: String,
    val spellId: EntityId,
    val casterId: EntityId
) : ContinuationFrame

/**
 * Resume after player selects cards for sacrifice.
 *
 * @property playerId The player who is sacrificing
 * @property sourceId The spell/ability that caused the sacrifice
 * @property sourceName Name of the source for event messages
 */
@Serializable
data class SacrificeContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?
) : ContinuationFrame

/**
 * Resume after player makes a yes/no choice (may abilities).
 *
 * @property playerId The player who made the choice
 * @property sourceId The spell/ability with the may clause
 * @property sourceName Name of the source
 * @property effectIfYes The effect to execute if player chose yes
 * @property effectIfNo The effect to execute if player chose no (usually null/no-op)
 */
@Serializable
data class MayAbilityContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val effectIfYes: Effect?,
    val effectIfNo: Effect?,
    val controllerId: EntityId,
    val opponentId: EntityId?,
    val xValue: Int?,
    val targets: List<ChosenTarget> = emptyList(),
    val triggeringEntityId: EntityId? = null,
    val triggerDamageAmount: Int? = null,
    val namedTargets: Map<String, ChosenTarget> = emptyMap()
) : ContinuationFrame {
    fun toEffectContext(): EffectContext = EffectContext(
        sourceId = sourceId,
        controllerId = controllerId,
        opponentId = opponentId,
        xValue = xValue,
        targets = targets,
        triggeringEntityId = triggeringEntityId,
        triggerDamageAmount = triggerDamageAmount,
        namedTargets = namedTargets
    )
}

/**
 * Resume after player selects cards to discard for hand size (cleanup step).
 *
 * This is separate from DiscardContinuation to distinguish hand size
 * discards from spell/ability-caused discards.
 *
 * @property playerId The player who is discarding
 */
@Serializable
data class HandSizeDiscardContinuation(
    override val decisionId: String,
    val playerId: EntityId
) : ContinuationFrame

/**
 * Resume after player selects a card from their graveyard.
 *
 * Used for spells like Elven Cache and Déjà Vu that let the player
 * choose a card from their graveyard to return to hand/battlefield.
 *
 * @property playerId The player who is searching their graveyard
 * @property sourceId The spell/ability that caused the search
 * @property sourceName Name of the source for event messages
 * @property destination Where to put the selected card (HAND or BATTLEFIELD)
 */
@Serializable
data class ReturnFromGraveyardContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val destination: SearchDestination
) : ContinuationFrame


/**
 * Resume after attacking player declares damage assignment order for blockers.
 *
 * Per MTG CR 509.2, after the defending player declares blockers, the attacking
 * player must declare the damage assignment order for each attacking creature
 * that's blocked by multiple creatures.
 *
 * @property attackingPlayerId The attacking player who must order the blockers
 * @property attackerId The attacking creature whose blockers are being ordered
 * @property attackerName Name of the attacker for display
 * @property remainingAttackers List of attackers that still need ordering after this one
 */
@Serializable
data class BlockerOrderContinuation(
    override val decisionId: String,
    val attackingPlayerId: EntityId,
    val attackerId: EntityId,
    val attackerName: String,
    val remainingAttackers: List<EntityId>
) : ContinuationFrame

/**
 * Resume after player selects cards/permanents for a generic "pay or suffer" effect.
 *
 * Used for unified "unless" mechanics like PayOrSufferEffect.
 *
 * @property playerId The player who must make the choice
 * @property sourceId The source that triggered this effect
 * @property sourceName Name of the source for event messages
 * @property costType The type of cost being paid (for dispatch to appropriate handler)
 * @property sufferEffect The effect to execute if the player doesn't pay
 * @property requiredCount Number of items required (cards to discard, permanents to sacrifice)
 * @property filter The filter for valid selections
 * @property random Whether the selection should be random (for Discard costs)
 */
@Serializable
data class PayOrSufferContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId,
    val sourceName: String,
    val costType: PayOrSufferCostType,
    val sufferEffect: Effect,
    val requiredCount: Int,
    val filter: GameObjectFilter,
    val random: Boolean = false,
    val targets: List<ChosenTarget> = emptyList(),
    val namedTargets: Map<String, ChosenTarget> = emptyMap()
) : ContinuationFrame

/**
 * Discriminator for the cost type in PayOrSufferContinuation.
 */
@Serializable
enum class PayOrSufferCostType {
    DISCARD,
    SACRIFICE,
    PAY_LIFE
}

/**
 * Resume after player has distributed damage among targets.
 *
 * Used for effects like Forked Lightning where the player divides damage
 * among multiple targets. The continuation is pushed when there are multiple
 * targets, and the response contains the damage distribution.
 *
 * @property sourceId The spell/ability that is dealing the damage
 * @property controllerId The player who controls the effect
 * @property targets The targets that damage can be distributed among
 */
@Serializable
data class DistributeDamageContinuation(
    override val decisionId: String,
    val sourceId: EntityId?,
    val controllerId: EntityId,
    val targets: List<EntityId>
) : ContinuationFrame

/**
 * Resume after player reorders cards for a MoveCollection with ControllerChooses order.
 *
 * When MoveCollectionEffect has order = CardOrder.ControllerChooses and there are
 * multiple cards going to the top of a library, we pause for the player to choose
 * the order. The response contains the card IDs in the new order (first = new top).
 *
 * @property playerId The player who is reordering
 * @property sourceId The spell/ability that caused this effect
 * @property sourceName Name of the source for event messages
 * @property cards The cards being reordered
 * @property destinationZone The zone the cards are going to
 * @property destinationPlayerId The player whose zone the cards go to
 */
@Serializable
data class MoveCollectionOrderContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val cards: List<EntityId>,
    val destinationZone: com.wingedsheep.sdk.core.Zone,
    val destinationPlayerId: EntityId,
    val placement: com.wingedsheep.sdk.scripting.effects.ZonePlacement = com.wingedsheep.sdk.scripting.effects.ZonePlacement.Top
) : ContinuationFrame

/**
 * Resume after player selects a target during a pipeline effect (mid-resolution targeting).
 *
 * Used by SelectTargetPipelineExecutor: the player has chosen a target from the
 * legal targets list. The selected target IDs are stored under [storeAs] and
 * injected into the next EffectContinuation's storedCollections.
 *
 * @property playerId The player who made the selection
 * @property sourceId The spell/ability that caused this effect
 * @property sourceName Name of the source for event messages
 * @property storeAs Name to store the selected target IDs under
 * @property storedCollections Snapshot of pipeline collections at time of pause
 */
@Serializable
data class SelectTargetPipelineContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val storeAs: String,
    val storedCollections: Map<String, List<EntityId>> = emptyMap()
) : ContinuationFrame

/**
 * Resume after player selects cards from a named pipeline collection.
 *
 * Used by SelectFromCollectionEffect: the player has chosen cards from a gathered
 * collection. The selected cards are stored under [storeSelected] and the remainder
 * (if [storeRemainder] is non-null) is stored under that name. Both collections are
 * injected into the next EffectContinuation's storedCollections.
 *
 * @property playerId The player who made the selection
 * @property sourceId The spell/ability that caused this effect
 * @property sourceName Name of the source for event messages
 * @property allCards All the cards in the collection being selected from
 * @property storeSelected Name to store the selected cards under
 * @property storeRemainder Name to store non-selected cards under (null = discard)
 * @property storedCollections Snapshot of pipeline collections at time of pause
 */
@Serializable
data class SelectFromCollectionContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val allCards: List<EntityId>,
    val storeSelected: String,
    val storeRemainder: String?,
    val storedCollections: Map<String, List<EntityId>> = emptyMap()
) : ContinuationFrame


/**
 * Resume after player chooses a color for protection granting effects.
 *
 * Used for effects like Akroma's Blessing: "Choose a color. Creatures you control
 * gain protection from the chosen color until end of turn."
 *
 * @property controllerId The player who controls the effect
 * @property sourceId The spell/ability that created this effect
 * @property sourceName Name of the source for event messages
 * @property filter Which creatures are affected
 * @property duration How long the effect lasts
 */
@Serializable
data class ChooseColorProtectionContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val filter: GroupFilter,
    val duration: Duration
) : ContinuationFrame

/**
 * Resume after player chooses a color for single-target protection granting effects.
 *
 * Used for effects like Jareth, Leonine Titan: "{W}: Jareth gains protection
 * from the color of your choice until end of turn."
 *
 * @property controllerId The player who controls the effect
 * @property sourceId The ability source that created this effect
 * @property sourceName Name of the source for event messages
 * @property targetEntityId The specific entity that gains protection
 * @property duration How long the effect lasts
 */
@Serializable
data class ChooseColorProtectionTargetContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val targetEntityId: EntityId,
    val duration: Duration
) : ContinuationFrame

/**
 * Resume after player chooses the FROM creature type for text replacement.
 *
 * Used for Artificial Evolution: "Change the text of target spell or permanent
 * by replacing all instances of one creature type with another."
 *
 * Step 1: Player chooses the creature type to replace.
 * The continuation handler then presents a second choice for the TO type.
 *
 * @property controllerId The player who controls the effect
 * @property sourceId The spell that created this effect
 * @property sourceName Name of the source for event messages
 * @property targetId The entity whose text is being changed
 * @property creatureTypes The creature type options presented (indexed by OptionChosenResponse.optionIndex)
 * @property excludedTypes Creature types that cannot be chosen as the TO type (e.g., "Wall" for Artificial Evolution)
 */
@Serializable
data class ChooseFromCreatureTypeContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val targetId: EntityId,
    val creatureTypes: List<String>,
    val excludedTypes: List<String> = emptyList()
) : ContinuationFrame

/**
 * Resume after player selects which permanents to keep tapped during untap step.
 *
 * Used for permanents with "You may choose not to untap" keyword (MAY_NOT_UNTAP).
 * The player selects which permanents to keep tapped; everything else untaps normally.
 *
 * @property playerId The active player making the choice
 * @property allPermanentsToUntap All permanents that would normally untap
 */
@Serializable
data class UntapChoiceContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val allPermanentsToUntap: List<EntityId>
) : ContinuationFrame

/**
 * Resume after player chooses the TO creature type for text replacement.
 *
 * Step 2: Player chooses the replacement creature type.
 * The continuation handler then applies the TextReplacementComponent.
 *
 * @property controllerId The player who controls the effect
 * @property sourceId The spell that created this effect
 * @property sourceName Name of the source for event messages
 * @property targetId The entity whose text is being changed
 * @property fromType The creature type being replaced (chosen in step 1)
 * @property creatureTypes The creature type options presented (indexed by OptionChosenResponse.optionIndex)
 */
@Serializable
data class ChooseToCreatureTypeContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val targetId: EntityId,
    val fromType: String,
    val creatureTypes: List<String>
) : ContinuationFrame

/**
 * Resume after player chooses a creature type in a pipeline context.
 *
 * Stores the chosen type into the EffectContinuation below on the stack
 * (via chosenCreatureType field) so subsequent pipeline effects can access it
 * via EffectContext.chosenCreatureType.
 *
 * @property controllerId The player choosing
 * @property sourceId The ability source
 * @property sourceName Name of the source for display
 * @property creatureTypes The creature type options (indexed by OptionChosenResponse.optionIndex)
 */
@Serializable
data class ChooseCreatureTypePipelineContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val creatureTypes: List<String>
) : ContinuationFrame

/**
 * Resume after player chooses an option in a generic pipeline context.
 *
 * Stores the chosen value into the EffectContinuation below on the stack
 * (via chosenValues map) so subsequent pipeline effects can access it
 * via EffectContext.chosenValues[storeAs].
 *
 * @property controllerId The player choosing
 * @property sourceId The ability source
 * @property sourceName Name of the source for display
 * @property storeAs Key under which to store the chosen value
 * @property options The option strings (indexed by OptionChosenResponse.optionIndex)
 */
@Serializable
data class ChooseOptionPipelineContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val storeAs: String,
    val options: List<String>
) : ContinuationFrame

/**
 * Resume after player chooses a creature type for a "becomes the creature type
 * of your choice" effect.
 *
 * @property controllerId The player who controls the spell/ability
 * @property sourceId The spell/ability that caused this effect
 * @property sourceName Name of the source for event messages
 * @property targetId The creature whose type will change
 * @property creatureTypes The creature type options (indexed by OptionChosenResponse.optionIndex)
 * @property duration How long the type change lasts
 */
@Serializable
data class BecomeCreatureTypeContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val targetId: EntityId,
    val creatureTypes: List<String>,
    val duration: Duration
) : ContinuationFrame

/**
 * Resume after the spell's controller decides whether to pay a mana cost
 * to prevent their spell from being countered.
 *
 * "Counter target spell unless its controller pays {cost}."
 *
 * @property payingPlayerId The spell's controller who must decide whether to pay
 * @property spellEntityId The spell that will be countered if they don't pay
 * @property manaCost The mana cost to pay
 * @property sourceId The source of the counter-unless-pays effect
 * @property sourceName Name of the source for event messages
 */
@Serializable
data class CounterUnlessPaysContinuation(
    override val decisionId: String,
    val payingPlayerId: EntityId,
    val spellEntityId: EntityId,
    val manaCost: ManaCost,
    val sourceId: EntityId?,
    val sourceName: String?
) : ContinuationFrame

/**
 * Resume after Meddle's controller chooses a new creature target for a spell.
 *
 * @property spellEntityId The spell whose target is being changed
 * @property sourceId The source of the change-target effect (Meddle)
 */
@Serializable
data class ChangeSpellTargetContinuation(
    override val decisionId: String,
    val spellEntityId: EntityId,
    val sourceId: EntityId?
) : ContinuationFrame

/**
 * Resume after player reorders revealed cards to put on the bottom of their library.
 *
 * Used for effects like Erratic Explosion that reveal cards and then put them
 * on the bottom of the library in any order.
 *
 * @property playerId The player whose library is being manipulated
 * @property sourceId The spell/ability that caused this
 * @property sourceName Name of the source for event messages
 */
@Serializable
data class PutOnBottomOfLibraryContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?
) : ContinuationFrame

/**
 * Resume after player chooses a mode for a modal spell/ability.
 *
 * When a modal effect (e.g., "Choose one —") is executed, the player is presented
 * with a list of modes. After they choose, we need to execute the chosen mode's
 * effect, potentially after target selection.
 *
 * @property controllerId The player who controls the spell/ability
 * @property sourceId The spell/ability that has the modal effect
 * @property sourceName Name of the source for event messages
 * @property modes The serialized modes (effects + target requirements)
 * @property xValue The X value if applicable
 * @property opponentId The opponent player ID
 */
@Serializable
data class ModalContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val modes: List<@Serializable Mode>,
    val xValue: Int? = null,
    val opponentId: EntityId? = null,
    val triggeringEntityId: EntityId? = null
) : ContinuationFrame

/**
 * Resume after player selects targets for a chosen mode of a modal spell.
 *
 * After mode selection, if the chosen mode requires targets, this continuation
 * is pushed while the player selects targets.
 *
 * @property controllerId The player who controls the spell/ability
 * @property sourceId The spell/ability that has the modal effect
 * @property sourceName Name of the source for event messages
 * @property effect The chosen mode's effect to execute
 * @property xValue The X value if applicable
 * @property opponentId The opponent player ID
 */
@Serializable
data class ModalTargetContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val effect: Effect,
    val xValue: Int? = null,
    val opponentId: EntityId? = null,
    val targetRequirements: List<TargetRequirement> = emptyList()
) : ContinuationFrame

/**
 * Resume after a player decides whether to pay a cost for "any player may [cost]" effects.
 *
 * Each player in APNAP order gets the chance to pay. If the current player pays,
 * the consequence is executed immediately. If they decline, we move to the next player.
 *
 * @property currentPlayerId The player currently being asked
 * @property remainingPlayers Players still to be asked after the current one
 * @property sourceId The source permanent
 * @property sourceName Name of the source for display
 * @property controllerId The controller of the source permanent
 * @property cost The cost being offered
 * @property consequence The effect to execute if any player pays
 * @property requiredCount Number of items required (for sacrifice costs)
 * @property filter The filter for valid selections (for sacrifice costs)
 */
@Serializable
data class AnyPlayerMayPayContinuation(
    override val decisionId: String,
    val currentPlayerId: EntityId,
    val remainingPlayers: List<EntityId>,
    val sourceId: EntityId,
    val sourceName: String,
    val controllerId: EntityId,
    val cost: PayCost,
    val consequence: Effect,
    val requiredCount: Int,
    val filter: GameObjectFilter
) : ContinuationFrame

/**
 * Resume after the controller decides whether to pay a mana cost for an optional
 * mana payment effect (e.g., Lightning Rift's "you may pay {1}").
 *
 * If the player pays, the inner effect is executed. If not, nothing happens.
 *
 * @property playerId The player who may pay
 * @property sourceId The source of the effect
 * @property sourceName Name of the source for display
 * @property manaCost The mana cost to pay
 * @property effect The effect to execute if the player pays
 * @property controllerId The controller for effect context
 * @property opponentId The opponent for effect context
 * @property xValue The X value if applicable
 * @property targets The chosen targets for effect context
 */
@Serializable
data class MayPayManaContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val manaCost: ManaCost,
    val effect: Effect,
    val controllerId: EntityId,
    val opponentId: EntityId?,
    val xValue: Int?,
    val targets: List<ChosenTarget> = emptyList(),
    val triggeringEntityId: EntityId? = null,
    val namedTargets: Map<String, ChosenTarget> = emptyMap()
) : ContinuationFrame {
    fun toEffectContext(): EffectContext = EffectContext(
        sourceId = sourceId,
        controllerId = controllerId,
        opponentId = opponentId,
        xValue = xValue,
        targets = targets,
        triggeringEntityId = triggeringEntityId,
        namedTargets = namedTargets
    )
}

/**
 * Resume after the controller decides whether to pay a mana cost for a triggered
 * ability that also requires targets (e.g., Lightning Rift).
 *
 * The flow is: ask "pay {cost}?" → if yes, show mana source selection → then target selection.
 * This is different from MayPayManaContinuation which asks about targets first.
 *
 * @property trigger The full pending trigger to process if the player pays
 * @property targetRequirement The target requirement for the ability
 * @property manaCost The mana cost to pay
 */
@Serializable
data class MayPayManaTriggerContinuation(
    override val decisionId: String,
    val trigger: PendingTrigger,
    val targetRequirement: TargetRequirement,
    val manaCost: ManaCost
) : ContinuationFrame

/**
 * Resume after player chooses a creature type for stat modification.
 *
 * Used for Defensive Maneuvers: "Creatures of the creature type of your choice get +0/+4
 * until end of turn."
 *
 * @property controllerId The player who controls the effect
 * @property sourceId The spell/ability that caused this effect
 * @property sourceName Name of the source for event messages
 * @property creatureTypes The creature type options (indexed by OptionChosenResponse.optionIndex)
 * @property powerModifier Power bonus
 * @property toughnessModifier Toughness bonus
 * @property duration How long the effect lasts
 */
@Serializable
data class ChooseCreatureTypeModifyStatsContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val creatureTypes: List<String>,
    val powerModifier: Int,
    val toughnessModifier: Int,
    val duration: Duration
) : ContinuationFrame

/**
 * Resume after the controller chooses a creature type to set ALL creatures' subtypes.
 *
 * Used by Standardize: "Choose a creature type other than Wall. Each creature becomes
 * that type until end of turn."
 *
 * @property controllerId The player who controls the effect
 * @property sourceId The spell/ability that caused this effect
 * @property sourceName Name of the source for event messages
 * @property creatureTypes The creature type options (indexed by OptionChosenResponse.optionIndex)
 * @property duration How long the effect lasts
 */
@Serializable
data class BecomeChosenTypeAllCreaturesContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val creatureTypes: List<String>,
    val duration: Duration
) : ContinuationFrame

/**
 * Resume after the controller selects mana sources to pay a cost for a triggered
 * ability that also requires targets.
 *
 * After mana sources are selected and cost is paid, proceeds to target selection.
 *
 * @property trigger The full pending trigger to process after payment
 * @property targetRequirement The target requirement for the ability
 * @property manaCost The mana cost to pay
 * @property availableSources Available mana sources the player can choose from
 * @property autoPaySuggestion Pre-computed auto-tap suggestion
 */
@Serializable
data class ManaSourceSelectionContinuation(
    override val decisionId: String,
    val trigger: PendingTrigger,
    val targetRequirement: TargetRequirement,
    val manaCost: ManaCost,
    val availableSources: List<ManaSourceOption>,
    val autoPaySuggestion: List<EntityId>
) : ContinuationFrame

/**
 * Resume after player selects a creature to copy for Clone-style effects.
 *
 * When a permanent with EntersAsCopy resolves, the player is asked to choose
 * a creature on the battlefield. This continuation handles the response
 * and completes the permanent's entry to the battlefield.
 *
 * @property spellId The spell entity being resolved
 * @property controllerId The player who cast the spell
 * @property ownerId The owner of the card
 * @property castFaceDown Whether the spell was cast face-down
 * @property optional Whether the copy is optional (Clone is optional)
 */
@Serializable
data class CloneEntersContinuation(
    override val decisionId: String,
    val spellId: EntityId,
    val controllerId: EntityId,
    val ownerId: EntityId,
    val castFaceDown: Boolean
) : ContinuationFrame

/**
 * Resume after player chooses a color for an "as enters, choose a color" effect.
 *
 * When a permanent with EntersWithColorChoice resolves, the player is asked to choose
 * a color. This continuation handles the response and stores the chosen color.
 * If the permanent also has EntersWithCreatureTypeChoice, it chains to that decision next.
 *
 * @property spellId The spell entity being resolved
 * @property controllerId The player who cast the spell
 * @property ownerId The owner of the card
 */
@Serializable
data class ChooseColorEntersContinuation(
    override val decisionId: String,
    val spellId: EntityId,
    val controllerId: EntityId,
    val ownerId: EntityId
) : ContinuationFrame

/**
 * Resume after player chooses a creature type for an "as enters, choose a creature type" effect.
 *
 * When a permanent with EntersWithCreatureTypeChoice resolves, the player is asked to choose
 * a creature type. This continuation handles the response and completes the permanent's
 * entry to the battlefield with the chosen type stored as a component.
 *
 * @property spellId The spell entity being resolved
 * @property controllerId The player who cast the spell
 * @property ownerId The owner of the card
 * @property creatureTypes The list of creature types presented to the player
 */
@Serializable
data class ChooseCreatureTypeEntersContinuation(
    override val decisionId: String,
    val spellId: EntityId,
    val controllerId: EntityId,
    val ownerId: EntityId,
    val creatureTypes: List<String>
) : ContinuationFrame

/**
 * Resume casting a spell after the player chooses a creature type during casting.
 *
 * Used for spells like Aphetto Dredging where the creature type choice is part of
 * casting (not resolution), so the opponent can see the chosen type on the stack.
 *
 * @property cardId The card being cast
 * @property casterId The player casting the spell
 * @property targets The chosen targets
 * @property xValue The X value if applicable
 * @property sacrificedPermanents Permanents sacrificed as additional costs
 * @property targetRequirements The target requirements for resolution-time re-validation
 * @property count Legacy field, unused by pipeline effects
 * @property creatureTypes The creature type options (indexed by OptionChosenResponse.optionIndex)
 */
@Serializable
data class CastWithCreatureTypeContinuation(
    override val decisionId: String,
    val cardId: EntityId,
    val casterId: EntityId,
    val targets: List<ChosenTarget> = emptyList(),
    val xValue: Int? = null,
    val sacrificedPermanents: List<EntityId> = emptyList(),
    val targetRequirements: List<TargetRequirement> = emptyList(),
    val count: Int,
    val creatureTypes: List<String>
) : ContinuationFrame

/**
 * Resume after a player selects a card to discard for "each player discards or lose life" effects.
 *
 * Used for Strongarm Tactics: "Each player discards a card. Then each player who didn't
 * discard a creature card this way loses 4 life."
 *
 * Tracks which players have already discarded and whether they discarded a creature,
 * then applies life loss to those who didn't.
 *
 * @property sourceId The spell/ability causing the effect
 * @property sourceName Name for display
 * @property controllerId The controller of the effect
 * @property currentPlayerId The player whose selection we are waiting for
 * @property remainingPlayers Players who still need to make their selection after current (APNAP order)
 * @property discardedCreature Map of player ID to whether they discarded a creature card
 * @property lifeLoss Life lost by each player who didn't discard a creature card
 */
@Serializable
data class EachPlayerDiscardsOrLoseLifeContinuation(
    override val decisionId: String,
    val sourceId: EntityId?,
    val sourceName: String?,
    val controllerId: EntityId,
    val currentPlayerId: EntityId,
    val remainingPlayers: List<EntityId>,
    val discardedCreature: Map<EntityId, Boolean>,
    val lifeLoss: Int
) : ContinuationFrame

/**
 * Resume after the controller chooses a target for an Aura being moved to the battlefield
 * via MoveCollectionEffect (atomic pipeline path).
 *
 * Per Rule 303.4f, when an Aura enters the battlefield without being cast, its controller
 * chooses what it enchants. Targeting restrictions (hexproof, shroud) do not apply.
 *
 * @property auraId The entity ID of the aura being placed
 * @property controllerId The player placing the aura (chooses target)
 * @property destPlayerId The player on whose battlefield the aura enters
 * @property remainingAuras More auras that need target selection
 * @property sourceId The source of the effect (for display)
 * @property sourceName Name of the source for display
 */
@Serializable
data class MoveCollectionAuraTargetContinuation(
    override val decisionId: String,
    val auraId: EntityId,
    val controllerId: EntityId,
    val destPlayerId: EntityId,
    val remainingAuras: List<EntityId>,
    val sourceId: EntityId?,
    val sourceName: String?
) : ContinuationFrame

/**
 * Resume after the controller chooses a creature type for "must attack this turn" effect.
 *
 * Used by Walking Desecration: "Creatures of the creature type of your choice attack
 * this turn if able."
 *
 * After the player chooses a creature type, all creatures of that type on the battlefield
 * are given MustAttackThisTurnComponent.
 *
 * @property controllerId The player who controls the effect
 * @property sourceId The permanent that created this effect
 * @property sourceName Name of the source for event messages
 * @property creatureTypes The creature type options (indexed by OptionChosenResponse.optionIndex)
 */
@Serializable
data class ChooseCreatureTypeMustAttackContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val creatureTypes: List<String>
) : ContinuationFrame

/**
 * Resume after player chooses a creature type for Peer Pressure-style effects.
 *
 * "Choose a creature type. If you control more creatures of that type than each
 * other player, you gain control of all creatures of that type."
 *
 * @property controllerId The player who controls the effect
 * @property sourceId The spell/ability that caused this effect
 * @property sourceName Name of the source for event messages
 * @property creatureTypes The creature type options (indexed by OptionChosenResponse.optionIndex)
 * @property duration How long the control change lasts
 */
@Serializable
data class ChooseCreatureTypeGainControlContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val creatureTypes: List<String>,
    val duration: Duration
) : ContinuationFrame

/**
 * Continuation for choosing a creature type and then untapping all creatures of that type.
 *
 * Used by Riptide Chronologist: "{U}, Sacrifice Riptide Chronologist:
 * Untap all creatures of the creature type of your choice."
 *
 * @property controllerId The player who controls the effect
 * @property sourceId The permanent that created this effect
 * @property sourceName Name of the source for event messages
 * @property creatureTypes The creature type options (indexed by OptionChosenResponse.optionIndex)
 */
@Serializable
data class ChooseCreatureTypeUntapContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val creatureTypes: List<String>
) : ContinuationFrame

/**
 * Resume after a player chose a creature type for "each player chooses a creature type" effects.
 *
 * Each player (in APNAP order) chooses a creature type. After all players have chosen,
 * the accumulated chosen types are stored in the EffectContinuation below via storedStringLists[storeAs].
 *
 * @property sourceId The spell that created this effect
 * @property sourceName Name of the source for display
 * @property controllerId The controller of the spell
 * @property currentPlayerId The player whose choice we are waiting for
 * @property remainingPlayers Players who still need to choose (APNAP order)
 * @property chosenTypes Creature types chosen so far by each player
 * @property creatureTypes The creature type options list
 * @property storeAs Key under which the chosen types are stored in storedStringLists
 */
@Serializable
data class EachPlayerChoosesCreatureTypeContinuation(
    override val decisionId: String,
    val sourceId: EntityId?,
    val sourceName: String?,
    val controllerId: EntityId,
    val currentPlayerId: EntityId,
    val remainingPlayers: List<EntityId>,
    val chosenTypes: List<String>,
    val creatureTypes: List<String>,
    val storeAs: String
) : ContinuationFrame

/**
 * Continuation for Patriarch's Bidding.
 * Each player chooses a creature type (APNAP order). After all choices, all creature cards
 * matching any chosen type are returned from all graveyards to the battlefield.
 *
 * @property currentPlayerId The player currently choosing
 * @property remainingPlayers Players who still need to choose (APNAP order)
 * @property chosenTypes Creature types chosen so far by each player
 * @property creatureTypes The creature type options list
 */
@Serializable
data class PatriarchsBiddingContinuation(
    override val decisionId: String,
    val sourceId: EntityId?,
    val sourceName: String?,
    val controllerId: EntityId,
    val currentPlayerId: EntityId,
    val remainingPlayers: List<EntityId>,
    val chosenTypes: List<String>,
    val creatureTypes: List<String>
) : ContinuationFrame

/**
 * Resume after primary discard effect needs card selection (Chain of Smog).
 *
 * When the card selection is answered, discards the selected cards, then offers
 * the chain copy via ChainCopyDecisionContinuation.
 *
 * @property effect The unified chain copy effect (carries all variant info)
 * @property playerId The player who is discarding
 * @property sourceId The source entity of the original spell/ability
 */
@Serializable
data class ChainCopyPrimaryDiscardContinuation(
    override val decisionId: String,
    val effect: ChainCopyEffect,
    val playerId: EntityId,
    val sourceId: EntityId?
) : ContinuationFrame

/**
 * Resume after the affected player decides whether to copy the chain spell (yes/no).
 *
 * - Yes → present cost payment (if any) or target selection
 * - No → chain ends
 *
 * @property effect The unified chain copy effect
 * @property copyControllerId The player who gets to copy
 * @property sourceId The source entity of the original spell/ability
 */
@Serializable
data class ChainCopyDecisionContinuation(
    override val decisionId: String,
    val effect: ChainCopyEffect,
    val copyControllerId: EntityId,
    val sourceId: EntityId?
) : ContinuationFrame

/**
 * Resume after the copying player selects a cost resource (land to sacrifice / card to discard).
 *
 * After paying cost, presents target selection for the copy.
 *
 * @property effect The unified chain copy effect
 * @property copyControllerId The player who is creating the copy
 * @property sourceId The source entity of the original spell/ability
 * @property candidateOptions The list of valid cost resource entity IDs (for validation)
 */
@Serializable
data class ChainCopyCostContinuation(
    override val decisionId: String,
    val effect: ChainCopyEffect,
    val copyControllerId: EntityId,
    val sourceId: EntityId?,
    val candidateOptions: List<EntityId>
) : ContinuationFrame

/**
 * Resume after the copying player selects a target for the chain copy.
 *
 * Creates a TriggeredAbilityOnStackComponent with ChainCopyEffect targeting
 * the selected entity, enabling recursive chaining.
 *
 * @property effect The unified chain copy effect
 * @property copyControllerId The player who is creating the copy
 * @property sourceId The source entity of the original spell/ability
 * @property candidateTargets The list of valid target entity IDs (for validation)
 */
@Serializable
data class ChainCopyTargetContinuation(
    override val decisionId: String,
    val effect: ChainCopyEffect,
    val copyControllerId: EntityId,
    val sourceId: EntityId?,
    val candidateTargets: List<EntityId>
) : ContinuationFrame

/**
 * Resume after defending player distributes damage prevention among multiple combat damage sources.
 *
 * Per CR 615.7, when a prevention effect can't prevent all simultaneous damage from multiple
 * sources, the affected player chooses how to distribute the prevention.
 *
 * @property recipientId The player/creature receiving damage
 * @property shieldEffectId The floating effect ID of the PreventNextDamage shield
 * @property shieldAmount Total prevention available from the shield
 * @property damageBySource Map of attacker entity ID → raw damage amount
 * @property firstStrike Whether this is during the first strike combat damage step
 */
@Serializable
data class DamagePreventionContinuation(
    override val decisionId: String,
    val recipientId: EntityId,
    val shieldEffectId: EntityId,
    val shieldAmount: Int,
    val damageBySource: Map<EntityId, Int>,
    val firstStrike: Boolean
) : ContinuationFrame

/**
 * Resume after a player chooses a number for secret bidding effects (Menacing Ogre).
 *
 * Each player secretly chooses a number. After all players have chosen,
 * each player with the highest number loses that much life. If the controller
 * is one of those players, put counters on the source creature.
 *
 * @property sourceId The creature that entered the battlefield
 * @property sourceName Name of the source for display
 * @property controllerId The controller of the source creature
 * @property currentPlayerId The player whose choice we are waiting for
 * @property remainingPlayers Players who still need to choose (APNAP order)
 * @property chosenNumbers Numbers chosen so far by each player
 * @property counterType Type of counter to add if controller has highest bid
 * @property counterCount Number of counters to add
 */
@Serializable
data class SecretBidContinuation(
    override val decisionId: String,
    val sourceId: EntityId?,
    val sourceName: String?,
    val controllerId: EntityId,
    val currentPlayerId: EntityId,
    val remainingPlayers: List<EntityId>,
    val chosenNumbers: Map<EntityId, Int>,
    val counterType: String,
    val counterCount: Int
) : ContinuationFrame

/**
 * Resume the draw step of a cycling action after cycling triggers have resolved.
 *
 * When cycling triggers (e.g., Choking Tethers' "you may tap target creature") pause
 * for player input, the CycleCardHandler returns early before reaching the draw step.
 * This continuation ensures the draw happens after triggers resolve.
 *
 * @property playerId The player who cycled and needs to draw
 */
@Serializable
data class CycleDrawContinuation(
    override val decisionId: String = "cycle-draw",
    val playerId: EntityId
) : ContinuationFrame

/**
 * Resume the search step of a typecycling action after cycling triggers have resolved.
 *
 * Same issue as CycleDrawContinuation but for typecycling, which searches the library
 * instead of drawing.
 *
 * @property playerId The player who typecycled
 * @property cardId The card that was typecycled (source for search effect)
 * @property subtypeFilter The creature subtype to search for
 */
@Serializable
data class TypecycleSearchContinuation(
    override val decisionId: String = "typecycle-search",
    val playerId: EntityId,
    val cardId: EntityId,
    val subtypeFilter: String
) : ContinuationFrame

/**
 * Resume remaining card draws after a bounce pipeline completes.
 *
 * When a draw is replaced by a bounce (Words of Wind), the pipeline handles the
 * "each player returns a permanent" part. This continuation tracks remaining draws
 * so execution can resume drawing after the pipeline finishes.
 *
 * @property drawingPlayerId The player who was drawing (whose draw was replaced)
 * @property remainingDraws Number of draws left to process after the bounce pipeline
 * @property isDrawStep Whether this is from the draw step (vs spell/ability draws)
 */
@Serializable
data class DrawReplacementRemainingDrawsContinuation(
    override val decisionId: String = "remaining-draws",
    val drawingPlayerId: EntityId,
    val remainingDraws: Int,
    val isDrawStep: Boolean
) : ContinuationFrame

/**
 * Continuation for prompting the player to activate a "prompt on draw" ability
 * (e.g., Words of Wind) before a draw happens.
 *
 * After the player answers yes/no, the handler pays mana, creates a replacement
 * shield, and then proceeds with the draw.
 *
 * @property drawingPlayerId The player who is about to draw
 * @property sourceId The permanent with the promptOnDraw ability
 * @property sourceName Name of the source for display
 * @property abilityEffect The effect to execute on activation (creates a shield)
 * @property manaCost The mana cost string for the activation (e.g., "{1}")
 * @property drawCount Number of cards to draw after activation
 * @property isDrawStep Whether this is from the draw step (vs spell/ability draws)
 */
@Serializable
data class DrawReplacementActivationContinuation(
    override val decisionId: String,
    val drawingPlayerId: EntityId,
    val sourceId: EntityId,
    val sourceName: String,
    val abilityEffect: Effect,
    val manaCost: String,
    val drawCount: Int,
    val isDrawStep: Boolean,
    val drawnCardsSoFar: List<EntityId> = emptyList(),
    val targetRequirements: List<com.wingedsheep.sdk.scripting.targets.TargetRequirement> = emptyList(),
    val declinedSourceIds: List<EntityId> = emptyList()
) : ContinuationFrame

/**
 * Resume after target selection for a "prompt on draw" ability that requires targeting
 * (e.g., Words of War). After the player paid mana and selected targets, we create
 * the replacement shield with the chosen targets, then proceed with draws.
 */
@Serializable
data class DrawReplacementTargetContinuation(
    override val decisionId: String,
    val drawingPlayerId: EntityId,
    val sourceId: EntityId,
    val sourceName: String,
    val abilityEffect: Effect,
    val drawCount: Int,
    val isDrawStep: Boolean,
    val drawnCardsSoFar: List<EntityId> = emptyList(),
    val targetRequirements: List<com.wingedsheep.sdk.scripting.targets.TargetRequirement> = emptyList()
) : ContinuationFrame


/**
 * Continuation for Read the Runes effect.
 * Tracks the iterative "for each card drawn, discard a card unless you sacrifice a permanent" choices.
 *
 * @property playerId The player making choices
 * @property sourceId The source spell
 * @property sourceName Name of the source spell
 * @property remainingChoices How many more discard-or-sacrifice choices remain
 * @property phase Whether we're awaiting a sacrifice selection or a discard selection
 */
@Serializable
data class ReadTheRunesContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val remainingChoices: Int,
    val phase: ReadTheRunesPhase
) : ContinuationFrame

/**
 * Phase discriminator for ReadTheRunesContinuation.
 */
@Serializable
enum class ReadTheRunesPhase {
    /** Player is choosing whether to sacrifice a permanent (select 0 to discard instead) */
    SACRIFICE_CHOICE,
    /** Player is choosing a card to discard */
    DISCARD_CHOICE
}

/**
 * Continuation for ForEachTargetEffect.
 *
 * When a sub-effect pipeline pauses for a decision during one iteration,
 * this continuation stores the remaining targets so execution continues
 * with the next target after the current pipeline completes.
 *
 * @property remainingTargets The targets still to process
 * @property effects The sub-effects to execute for each remaining target
 * @property sourceId The spell that caused this effect
 * @property controllerId The controller of the effect
 * @property opponentId The opponent (if applicable)
 * @property xValue The X value (if applicable)
 */
@Serializable
data class ForEachTargetContinuation(
    override val decisionId: String,
    val remainingTargets: List<ChosenTarget>,
    val effects: List<Effect>,
    val sourceId: EntityId?,
    val controllerId: EntityId,
    val opponentId: EntityId?,
    val xValue: Int?,
    val namedTargets: Map<String, ChosenTarget> = emptyMap()
) : ContinuationFrame

/**
 * Continuation for ForEachPlayerEffect.
 *
 * When a sub-effect pipeline pauses for a decision during one iteration,
 * this continuation stores the remaining players so execution continues
 * with the next player after the current pipeline completes.
 *
 * @property remainingPlayers The players still to process
 * @property effects The sub-effects to execute for each remaining player
 * @property sourceId The spell that caused this effect
 * @property controllerId The original controller of the effect
 * @property opponentId The opponent (if applicable)
 * @property xValue The X value (if applicable)
 */
@Serializable
data class ForEachPlayerContinuation(
    override val decisionId: String,
    val remainingPlayers: List<EntityId>,
    val effects: List<Effect>,
    val sourceId: EntityId?,
    val controllerId: EntityId,
    val opponentId: EntityId?,
    val xValue: Int?,
    val storedStringLists: Map<String, List<String>> = emptyMap()
) : ContinuationFrame

/**
 * Resume after a player chooses how many cards to draw for DrawUpToEffect.
 *
 * @property playerId The player who is drawing
 * @property sourceId The spell/ability that caused the effect
 * @property sourceName Name of the source for display
 * @property maxCards Maximum cards offered (capped by library size)
 */
@Serializable
data class DrawUpToContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val maxCards: Int,
    val originalMaxCards: Int = 0,
    val storeNotDrawnAs: String? = null
) : ContinuationFrame

/**
 * Continuation for RepeatWhileEffect.
 *
 * Stores the full body effect and repeat condition so the loop can re-execute.
 * For PlayerChooses conditions, the decider is resolved once and stored as
 * resolvedDeciderId to avoid re-resolving EffectTarget on subsequent iterations.
 *
 * Two phases:
 * - AFTER_BODY: Pre-pushed before body executes. Found in checkForMoreContinuations
 *   after the body (or its sub-effects) complete. Transitions to asking the condition.
 * - AFTER_DECISION: Waiting for the player's yes/no answer (PlayerChooses only).
 *
 * @property body The effect to execute each iteration
 * @property repeatCondition The serialized repeat condition
 * @property resolvedDeciderId For PlayerChooses — the resolved player entity ID
 * @property sourceId The spell/ability that caused this effect
 * @property sourceName Name of the source for display
 * @property controllerId The controller of the effect
 * @property opponentId The opponent (for effect context)
 * @property xValue The X value (if applicable)
 * @property targets The chosen targets (for effect context)
 * @property phase Current phase of the repeat loop
 */
@Serializable
data class RepeatWhileContinuation(
    override val decisionId: String,
    val body: Effect,
    val repeatCondition: com.wingedsheep.sdk.scripting.effects.RepeatCondition,
    val resolvedDeciderId: EntityId? = null,
    val sourceId: EntityId?,
    val sourceName: String?,
    val controllerId: EntityId,
    val opponentId: EntityId?,
    val xValue: Int?,
    val targets: List<ChosenTarget> = emptyList(),
    val phase: RepeatWhilePhase,
    val namedTargets: Map<String, ChosenTarget> = emptyMap()
) : ContinuationFrame {
    fun toEffectContext(): EffectContext = EffectContext(
        sourceId = sourceId,
        controllerId = controllerId,
        opponentId = opponentId,
        xValue = xValue,
        targets = targets,
        namedTargets = namedTargets
    )
}

/**
 * Phase discriminator for RepeatWhileContinuation.
 */
@Serializable
enum class RepeatWhilePhase {
    /** Pre-pushed before body executes; found in checkForMoreContinuations after body completes */
    AFTER_BODY,
    /** Waiting for the player's yes/no decision (PlayerChooses only) */
    AFTER_DECISION
}

/**
 * Information about a mana source available for manual selection.
 */
@Serializable
data class ManaSourceOption(
    val entityId: EntityId,
    val name: String,
    val producesColors: Set<Color>,
    val producesColorless: Boolean
)
