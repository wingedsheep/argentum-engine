package com.wingedsheep.engine.core

import com.wingedsheep.engine.event.PendingTrigger
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.Effect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GroupFilter
import com.wingedsheep.sdk.scripting.SearchDestination
import com.wingedsheep.sdk.targeting.TargetRequirement
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
 */
@Serializable
data class DiscardContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?
) : ContinuationFrame

/**
 * Resume after player orders cards for scry.
 *
 * @property playerId The player who is scrying
 * @property sourceId The spell/ability that caused the scry
 * @property sourceName Name of the source for event messages
 */
@Serializable
data class ScryContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?
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
    val xValue: Int?
) : ContinuationFrame {
    /**
     * Reconstruct the EffectContext from serialized fields.
     */
    fun toEffectContext(): EffectContext = EffectContext(
        sourceId = sourceId,
        controllerId = controllerId,
        opponentId = opponentId,
        xValue = xValue
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
    val triggeringEntityId: EntityId? = null
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
 * Resume combat damage assignment.
 *
 * @property attackerId The attacking creature assigning damage
 * @property defendingPlayerId The defending player
 */
@Serializable
data class DamageAssignmentContinuation(
    override val decisionId: String,
    val attackerId: EntityId,
    val defendingPlayerId: EntityId
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
    val targets: List<ChosenTarget> = emptyList()
) : ContinuationFrame {
    fun toEffectContext(): EffectContext = EffectContext(
        sourceId = sourceId,
        controllerId = controllerId,
        opponentId = opponentId,
        xValue = xValue,
        targets = targets
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
 * Resume after a player selects cards for "each player selects, then draws" effects.
 *
 * Used for effects like Flux, Windfall, etc. where each player selects cards
 * (which get discarded when processed) and then draws based on how many they selected.
 *
 * The continuation tracks pending draws and remaining players. When a player's
 * selection is processed, their cards are discarded and their draw count is recorded.
 * After all players have selected, draws are executed.
 *
 * @property sourceId The spell/ability causing the effect
 * @property sourceName Name for display
 * @property controllerId The controller of the effect
 * @property currentPlayerId The player whose selection we are waiting for
 * @property remainingPlayers Players who still need to make their selection after current (APNAP order)
 * @property drawAmounts How many cards each completed player will draw
 * @property controllerBonusDraw Extra cards the controller draws after the effect
 * @property minSelection Minimum cards each player must select (0 for "any number")
 * @property maxSelection Maximum cards each player can select (null means up to hand size)
 * @property selectionPrompt Prompt to show players when selecting
 */
@Serializable
data class EachPlayerSelectsThenDrawsContinuation(
    override val decisionId: String,
    val sourceId: EntityId?,
    val sourceName: String?,
    val controllerId: EntityId,
    val currentPlayerId: EntityId,
    val remainingPlayers: List<EntityId>,
    val drawAmounts: Map<EntityId, Int>,
    val controllerBonusDraw: Int,
    val minSelection: Int,
    val maxSelection: Int?,
    val selectionPrompt: String
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
 * Resume after player selects cards from library search.
 *
 * @property playerId The player who is searching
 * @property sourceId The spell/ability that caused the search
 * @property sourceName Name of the source for event messages
 * @property filter The card filter that was used
 * @property count Maximum cards that could be selected
 * @property destination Where to put the selected cards
 * @property entersTapped Whether permanents enter tapped (for battlefield destination)
 * @property shuffleAfter Whether to shuffle the library after search
 * @property reveal Whether to reveal the selected cards
 */
@Serializable
data class SearchLibraryContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val filter: GameObjectFilter,
    val count: Int,
    val destination: SearchDestination,
    val entersTapped: Boolean,
    val shuffleAfter: Boolean,
    val reveal: Boolean
) : ContinuationFrame

/**
 * Resume after player reorders cards on top of their library.
 *
 * Used for "look at the top N cards and put them back in any order" effects.
 * The response contains the cards in the new order (first = new top of library).
 *
 * @property playerId The player who looked at the cards
 * @property sourceId The spell/ability that caused this
 * @property sourceName Name of the source for event messages
 * @property originalCards The card IDs that were being reordered (for validation)
 */
@Serializable
data class ReorderLibraryContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?
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
 * Resume after a player chooses how many cards to draw for "each player may draw" effects.
 *
 * Used for effects like Temporary Truce where each player chooses how many cards (0-N)
 * to draw, and gains life for each card not drawn.
 *
 * The continuation tracks pending draws/life gains and remaining players. When a player's
 * choice is processed, their draw count and life gain are recorded. After all players
 * have chosen, draws and life gains are executed.
 *
 * @property sourceId The spell/ability causing the effect
 * @property sourceName Name for display
 * @property controllerId The controller of the effect
 * @property currentPlayerId The player whose choice we are waiting for
 * @property remainingPlayers Players who still need to choose after current (APNAP order)
 * @property drawAmounts How many cards each completed player will draw
 * @property lifeGainAmounts How much life each completed player will gain
 * @property maxCards Maximum cards each player may choose to draw
 * @property lifePerCardNotDrawn Life gained for each card not drawn (0 to disable)
 */
@Serializable
data class EachPlayerChoosesDrawContinuation(
    override val decisionId: String,
    val sourceId: EntityId?,
    val sourceName: String?,
    val controllerId: EntityId,
    val currentPlayerId: EntityId,
    val remainingPlayers: List<EntityId>,
    val drawAmounts: Map<EntityId, Int>,
    val lifeGainAmounts: Map<EntityId, Int>,
    val maxCards: Int,
    val lifePerCardNotDrawn: Int
) : ContinuationFrame

/**
 * Resume after player selected cards to put in opponent's graveyard from their library.
 *
 * Used for effects like Cruel Fate: "Look at the top N cards of target opponent's library.
 * Put X of them into that player's graveyard and the rest on top of their library in any order."
 *
 * This is a two-step continuation:
 * 1. First, player selects which cards go to graveyard (this continuation handles that response)
 * 2. Then, if there are remaining cards, player reorders them for the top of library
 *
 * @property playerId The player making the selections (controller of the effect)
 * @property opponentId The opponent whose library is being manipulated
 * @property sourceId The spell/ability that caused this
 * @property sourceName Name of the source for event messages
 * @property allCards All the cards that were looked at (for validation)
 * @property toGraveyard Number of cards that must go to graveyard
 */
@Serializable
data class LookAtOpponentLibraryContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val opponentId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val allCards: List<EntityId>,
    val toGraveyard: Int
) : ContinuationFrame

/**
 * Resume after player reordered the remaining cards to put back on opponent's library.
 *
 * Second step of LookAtOpponentLibraryEffect - after selecting cards for graveyard,
 * the remaining cards need to be reordered and put back on top.
 *
 * @property playerId The player making the reorder decision
 * @property opponentId The opponent whose library is being manipulated
 * @property sourceId The spell/ability that caused this
 * @property sourceName Name of the source for event messages
 */
@Serializable
data class ReorderOpponentLibraryContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val opponentId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?
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
    val random: Boolean = false
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
 * Resume after player selects cards to keep from looking at top cards of library.
 *
 * Used for effects like Ancestral Memories: "Look at the top N cards of your library.
 * Put X of them into your hand and the rest into your graveyard."
 *
 * @property playerId The player who is looking at and selecting cards
 * @property sourceId The spell/ability that caused this effect
 * @property sourceName Name of the source for event messages
 * @property allCards All the cards that were looked at (for validation and moving non-selected)
 * @property keepCount Number of cards the player must keep (put in hand)
 * @property restToGraveyard If true, non-selected cards go to graveyard; if false, they stay on top of library
 */
@Serializable
data class LookAtTopCardsContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val allCards: List<EntityId>,
    val keepCount: Int,
    val restToGraveyard: Boolean
) : ContinuationFrame

/**
 * Resume after an opponent chooses a card from revealed top cards.
 *
 * Used for effects like Animal Magnetism: "Reveal the top five cards of your library.
 * An opponent chooses a creature card from among them. Put that card onto the battlefield
 * and the rest into your graveyard."
 *
 * @property controllerId The player who cast the spell (owns the library and battlefield)
 * @property opponentId The opponent making the choice
 * @property sourceId The spell/ability that caused this effect
 * @property sourceName Name of the source for event messages
 * @property allCards All the revealed cards
 * @property creatureCards The subset of cards matching the filter (valid choices)
 */
@Serializable
data class RevealAndOpponentChoosesContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val opponentId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val allCards: List<EntityId>,
    val creatureCards: List<EntityId>
) : ContinuationFrame

/**
 * Resume after player chooses a creature type for graveyard retrieval.
 *
 * Used for Aphetto Dredging: "Return up to three target creature cards of the creature type
 * of your choice from your graveyard to your hand."
 *
 * Step 1: Player chooses a creature type from types present in their graveyard.
 * The continuation handler then presents a card selection for cards of that type.
 *
 * @property controllerId The player who cast the spell
 * @property sourceId The spell/ability that caused this effect
 * @property sourceName Name of the source for event messages
 * @property count Maximum number of cards to return
 * @property creatureTypes The creature type options presented (indexed by OptionChosenResponse.optionIndex)
 */
@Serializable
data class ChooseCreatureTypeReturnContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val count: Int,
    val creatureTypes: List<String>
) : ContinuationFrame

/**
 * Resume after player selects cards from graveyard to return to hand.
 *
 * Step 2 of the choose-type-then-return flow. Moves selected cards from graveyard to hand.
 *
 * @property controllerId The player whose graveyard and hand are involved
 * @property sourceId The spell/ability that caused this effect
 * @property sourceName Name of the source for event messages
 */
@Serializable
data class GraveyardToHandContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?
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
 * Resume after target player reveals cards for Blackmail-style effects.
 *
 * Step 1: Target player has selected cards to reveal (or all were auto-revealed).
 * Now the controller must choose one of the revealed cards for the target to discard.
 *
 * @property controllerId The player who cast Blackmail (chooses which card to discard)
 * @property targetPlayerId The target player whose hand is being revealed
 * @property sourceId The spell that caused this effect
 * @property sourceName Name of the source for display
 * @property revealedCards The cards that were revealed (controller picks from these)
 */
@Serializable
data class BlackmailRevealContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val targetPlayerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val revealedCards: List<EntityId>
) : ContinuationFrame

/**
 * Resume after controller chooses a card from revealed hand for Blackmail-style effects.
 *
 * Step 2: Controller has chosen a card. Now discard it from the target player's hand.
 *
 * @property targetPlayerId The target player who discards the chosen card
 * @property sourceId The spell that caused this effect
 * @property sourceName Name of the source for display
 */
@Serializable
data class BlackmailChooseContinuation(
    override val decisionId: String,
    val targetPlayerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?
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
