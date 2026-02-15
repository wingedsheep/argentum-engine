package com.wingedsheep.engine.core

import com.wingedsheep.engine.event.PendingTrigger
import com.wingedsheep.engine.handlers.EffectContext
import com.wingedsheep.engine.state.components.stack.ChosenTarget
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.Effect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GroupFilter
import com.wingedsheep.sdk.scripting.PayCost
import com.wingedsheep.sdk.scripting.SearchDestination
import com.wingedsheep.sdk.scripting.TargetFilter
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
 * Resume after player splits cards for surveil.
 *
 * Surveil N - Look at the top N cards of your library, then put any number of them
 * into your graveyard and the rest on top of your library in any order.
 *
 * Uses a SplitPilesDecision with two piles:
 * - Pile 0 = top of library (in order)
 * - Pile 1 = graveyard
 *
 * @property playerId The player who is surveilling
 * @property sourceId The spell/ability that caused the surveil
 * @property sourceName Name of the source for event messages
 */
@Serializable
data class SurveilContinuation(
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
    val triggerDamageAmount: Int? = null
) : ContinuationFrame {
    fun toEffectContext(): EffectContext = EffectContext(
        sourceId = sourceId,
        controllerId = controllerId,
        opponentId = opponentId,
        xValue = xValue,
        targets = targets,
        triggeringEntityId = triggeringEntityId,
        triggerDamageAmount = triggerDamageAmount
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
    val random: Boolean = false,
    val targets: List<ChosenTarget> = emptyList()
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
 * Resume after controller selects cards from opponent's library for Head Games.
 *
 * The controller has searched the opponent's library and selected cards.
 * Move selected cards to opponent's hand, then shuffle opponent's library.
 *
 * @property controllerId The player who cast Head Games (searching the library)
 * @property targetPlayerId The opponent whose library is being searched
 * @property sourceId The spell that caused this effect
 * @property sourceName Name of the source for display
 * @property searchCount The number of cards the controller can select (original hand size)
 */
@Serializable
data class HeadGamesContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val targetPlayerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val searchCount: Int
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
 * Resume after player chooses a creature type for Bloodline Shaman-style effects.
 *
 * "Choose a creature type. Reveal the top card of your library.
 * If that card is a creature card of the chosen type, put it into your hand.
 * Otherwise, put it into your graveyard."
 *
 * @property controllerId The player who activated the ability
 * @property sourceId The source permanent
 * @property sourceName Name of the source for event messages
 * @property creatureTypes The creature type options (indexed by OptionChosenResponse.optionIndex)
 */
@Serializable
data class ChooseCreatureTypeRevealTopContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val creatureTypes: List<String>
) : ContinuationFrame

/**
 * Resume after player chooses a creature type for "reveal until creature type" effects.
 *
 * "Choose a creature type. Reveal cards from the top of your library until you reveal
 * a creature card of that type. Put that card onto the battlefield and shuffle the rest
 * into your library."
 *
 * @property controllerId The player who activated the ability
 * @property sourceId The source permanent
 * @property sourceName Name of the source for event messages
 * @property creatureTypes The creature type options (indexed by OptionChosenResponse.optionIndex)
 */
@Serializable
data class RevealUntilCreatureTypeContinuation(
    override val decisionId: String,
    val controllerId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val creatureTypes: List<String>
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
    val modes: List<@Serializable com.wingedsheep.sdk.scripting.Mode>,
    val xValue: Int? = null,
    val opponentId: EntityId? = null
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
    val opponentId: EntityId? = null
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
    val triggeringEntityId: EntityId? = null
) : ContinuationFrame {
    fun toEffectContext(): EffectContext = EffectContext(
        sourceId = sourceId,
        controllerId = controllerId,
        opponentId = opponentId,
        xValue = xValue,
        targets = targets,
        triggeringEntityId = triggeringEntityId
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
 * @property count Maximum number of cards to return (for ChooseCreatureTypeReturnFromGraveyardEffect)
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
 * Resume after an opponent selects cards from their hand to put onto the battlefield.
 *
 * Used for Tempting Wurm: "Each opponent may put any number of artifact, creature,
 * enchantment, and/or land cards from their hand onto the battlefield."
 *
 * Each opponent in APNAP order is presented with a card selection. After one opponent
 * finishes, the next is asked.
 *
 * @property currentOpponentId The opponent currently selecting cards
 * @property remainingOpponents Opponents still to be asked after the current one
 * @property sourceId The source permanent
 * @property sourceName Name of the source for display
 * @property controllerId The controller of the source
 * @property filter The filter for valid cards from hand
 */
@Serializable
data class EachOpponentMayPutFromHandContinuation(
    override val decisionId: String,
    val currentOpponentId: EntityId,
    val remainingOpponents: List<EntityId>,
    val sourceId: EntityId?,
    val sourceName: String?,
    val controllerId: EntityId,
    val filter: GameObjectFilter
) : ContinuationFrame

/**
 * Resume after a player selects creature cards from their hand to reveal.
 *
 * Used for Kamahl's Summons: "Each player may reveal any number of creature cards
 * from their hand. Then each player creates a 2/2 green Bear creature token for
 * each card they revealed this way."
 *
 * Each player in APNAP order is presented with a card selection. After all players
 * have made their selection, tokens are created for each player based on their reveal count.
 *
 * @property currentPlayerId The player currently selecting cards to reveal
 * @property remainingPlayers Players still to be asked after the current one
 * @property sourceId The source spell/ability
 * @property sourceName Name of the source for display
 * @property revealCounts Map of player ID to number of cards they revealed
 * @property tokenPower Power of the created tokens
 * @property tokenToughness Toughness of the created tokens
 * @property tokenColors Colors of the created tokens
 * @property tokenCreatureTypes Creature types of the created tokens
 */
@Serializable
data class EachPlayerMayRevealCreaturesContinuation(
    override val decisionId: String,
    val currentPlayerId: EntityId,
    val remainingPlayers: List<EntityId>,
    val sourceId: EntityId?,
    val sourceName: String?,
    val revealCounts: Map<EntityId, Int>,
    val tokenPower: Int,
    val tokenToughness: Int,
    val tokenColors: Set<com.wingedsheep.sdk.core.Color>,
    val tokenCreatureTypes: Set<String>
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
 * Resume after a player chose a creature type for Harsh Mercy.
 *
 * Each player (in APNAP order) chooses a creature type. After all players have chosen,
 * destroy all creatures that aren't of any chosen type (can't be regenerated).
 *
 * @property sourceId The spell that created this effect
 * @property sourceName Name of the source for display
 * @property controllerId The controller of the spell
 * @property currentPlayerId The player whose choice we are waiting for
 * @property remainingPlayers Players who still need to choose (APNAP order)
 * @property chosenTypes Creature types chosen so far by each player
 * @property creatureTypes The creature type options list
 */
@Serializable
data class HarshMercyContinuation(
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
 * Resume after the destroyed permanent's controller decides whether to copy Chain of Acid.
 *
 * When the yes/no decision is answered:
 * - Yes → find legal noncreature permanents, present target selection, push ChainCopyTargetContinuation
 * - No → checkForMoreContinuations (chain ends)
 *
 * @property targetControllerId The controller of the destroyed permanent (who gets to copy)
 * @property targetFilter The filter for valid chain targets (NoncreaturePermanent)
 * @property spellName The name of the spell being copied (for display)
 * @property sourceId The source entity of the original spell/ability
 */
@Serializable
data class ChainCopyDecisionContinuation(
    override val decisionId: String,
    val targetControllerId: EntityId,
    val targetFilter: TargetFilter,
    val spellName: String,
    val sourceId: EntityId?
) : ContinuationFrame

/**
 * Resume after the copying player selects a target for the chain copy.
 *
 * Creates a TriggeredAbilityOnStackComponent with DestroyAndChainCopyEffect targeting
 * the selected permanent, enabling recursive chaining.
 *
 * @property copyControllerId The player who is creating the copy
 * @property targetFilter The filter for valid targets
 * @property spellName The name of the spell being copied
 * @property sourceId The source entity of the original spell/ability
 * @property candidateTargets The list of valid target entity IDs (for validation)
 */
@Serializable
data class ChainCopyTargetContinuation(
    override val decisionId: String,
    val copyControllerId: EntityId,
    val targetFilter: TargetFilter,
    val spellName: String,
    val sourceId: EntityId?,
    val candidateTargets: List<EntityId>
) : ContinuationFrame

/**
 * Resume after the bounced permanent's controller decides whether to sacrifice a land
 * to copy Chain of Vapor.
 *
 * When the yes/no decision is answered:
 * - Yes → find controller's lands, present land selection, push BounceChainCopyLandContinuation
 * - No → checkForMoreContinuations (chain ends)
 *
 * @property targetControllerId The controller of the bounced permanent (who gets to copy)
 * @property targetFilter The filter for valid chain targets (NonlandPermanent)
 * @property spellName The name of the spell being copied (for display)
 * @property sourceId The source entity of the original spell/ability
 */
@Serializable
data class BounceChainCopyDecisionContinuation(
    override val decisionId: String,
    val targetControllerId: EntityId,
    val targetFilter: TargetFilter,
    val spellName: String,
    val sourceId: EntityId?
) : ContinuationFrame

/**
 * Resume after the copying player selects which land to sacrifice for the chain copy.
 *
 * Sacrifices the selected land, then presents target selection for the copy.
 *
 * @property copyControllerId The player who is creating the copy
 * @property targetFilter The filter for valid targets
 * @property spellName The name of the spell being copied
 * @property sourceId The source entity of the original spell/ability
 * @property candidateLands The list of valid land entity IDs (for validation)
 */
@Serializable
data class BounceChainCopyLandContinuation(
    override val decisionId: String,
    val copyControllerId: EntityId,
    val targetFilter: TargetFilter,
    val spellName: String,
    val sourceId: EntityId?,
    val candidateLands: List<EntityId>
) : ContinuationFrame

/**
 * Resume after the copying player selects a target for the bounce chain copy.
 *
 * Creates a TriggeredAbilityOnStackComponent with BounceAndChainCopyEffect targeting
 * the selected permanent, enabling recursive chaining.
 *
 * @property copyControllerId The player who is creating the copy
 * @property targetFilter The filter for valid targets
 * @property spellName The name of the spell being copied
 * @property sourceId The source entity of the original spell/ability
 * @property candidateTargets The list of valid target entity IDs (for validation)
 */
@Serializable
data class BounceChainCopyTargetContinuation(
    override val decisionId: String,
    val copyControllerId: EntityId,
    val targetFilter: TargetFilter,
    val spellName: String,
    val sourceId: EntityId?,
    val candidateTargets: List<EntityId>
) : ContinuationFrame

/**
 * Resume after the target creature's controller decides whether to sacrifice a land
 * to copy Chain of Silence (prevent damage chain).
 *
 * @property targetControllerId The controller of the target creature (who gets to copy)
 * @property targetFilter The filter for valid chain targets (Creature)
 * @property spellName The name of the spell being copied (for display)
 * @property sourceId The source entity of the original spell/ability
 */
@Serializable
data class PreventDamageChainCopyDecisionContinuation(
    override val decisionId: String,
    val targetControllerId: EntityId,
    val targetFilter: TargetFilter,
    val spellName: String,
    val sourceId: EntityId?
) : ContinuationFrame

/**
 * Resume after the copying player selects which land to sacrifice for the prevent damage chain copy.
 *
 * @property copyControllerId The player who is creating the copy
 * @property targetFilter The filter for valid targets
 * @property spellName The name of the spell being copied
 * @property sourceId The source entity of the original spell/ability
 * @property candidateLands The list of valid land entity IDs (for validation)
 */
@Serializable
data class PreventDamageChainCopyLandContinuation(
    override val decisionId: String,
    val copyControllerId: EntityId,
    val targetFilter: TargetFilter,
    val spellName: String,
    val sourceId: EntityId?,
    val candidateLands: List<EntityId>
) : ContinuationFrame

/**
 * Resume after the copying player selects a target for the prevent damage chain copy.
 *
 * @property copyControllerId The player who is creating the copy
 * @property targetFilter The filter for valid targets
 * @property spellName The name of the spell being copied
 * @property sourceId The source entity of the original spell/ability
 * @property candidateTargets The list of valid target entity IDs (for validation)
 */
@Serializable
data class PreventDamageChainCopyTargetContinuation(
    override val decisionId: String,
    val copyControllerId: EntityId,
    val targetFilter: TargetFilter,
    val spellName: String,
    val sourceId: EntityId?,
    val candidateTargets: List<EntityId>
) : ContinuationFrame

/**
 * Resume after player selects cards to discard for a chain-copy spell (Chain of Smog).
 *
 * When the card selection is answered, discards the selected cards, then presents
 * a yes/no decision to copy the spell (pushes DiscardChainCopyDecisionContinuation).
 *
 * @property playerId The player who is discarding
 * @property count Number of cards to discard
 * @property spellName The name of the spell being copied (for display)
 * @property sourceId The source entity of the original spell/ability
 */
@Serializable
data class DiscardForChainContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val count: Int,
    val spellName: String,
    val sourceId: EntityId?
) : ContinuationFrame

/**
 * Resume after the discarding player decides whether to copy Chain of Smog.
 *
 * When the yes/no decision is answered:
 * - Yes → present target selection for copy, push DiscardChainCopyTargetContinuation
 * - No → checkForMoreContinuations (chain ends)
 *
 * @property targetPlayerId The player who discarded (who gets to copy)
 * @property count Number of cards the copy will make the target discard
 * @property spellName The name of the spell being copied (for display)
 * @property sourceId The source entity of the original spell/ability
 */
@Serializable
data class DiscardChainCopyDecisionContinuation(
    override val decisionId: String,
    val targetPlayerId: EntityId,
    val count: Int,
    val spellName: String,
    val sourceId: EntityId?
) : ContinuationFrame

/**
 * Resume after the copying player selects a target player for the discard chain copy.
 *
 * Creates a TriggeredAbilityOnStackComponent with DiscardAndChainCopyEffect targeting
 * the selected player, enabling recursive chaining.
 *
 * @property copyControllerId The player who is creating the copy
 * @property count Number of cards the copy will make the target discard
 * @property spellName The name of the spell being copied
 * @property sourceId The source entity of the original spell/ability
 * @property candidateTargets The list of valid target player entity IDs (for validation)
 */
@Serializable
data class DiscardChainCopyTargetContinuation(
    override val decisionId: String,
    val copyControllerId: EntityId,
    val count: Int,
    val spellName: String,
    val sourceId: EntityId?,
    val candidateTargets: List<EntityId>
) : ContinuationFrame

/**
 * Resume after the damaged player/permanent's controller decides whether to discard
 * a card to copy Chain of Plasma.
 *
 * When the yes/no decision is answered:
 * - Yes → present card selection for discard, push DamageChainDiscardContinuation
 * - No → checkForMoreContinuations (chain ends)
 *
 * @property affectedPlayerId The player who was damaged or whose permanent was damaged
 * @property amount The damage amount for the copy
 * @property spellName The name of the spell being copied
 * @property sourceId The source entity of the original spell/ability
 */
@Serializable
data class DamageChainCopyDecisionContinuation(
    override val decisionId: String,
    val affectedPlayerId: EntityId,
    val amount: Int,
    val spellName: String,
    val sourceId: EntityId?
) : ContinuationFrame

/**
 * Resume after the affected player selects a card to discard for the chain copy.
 *
 * After discarding, presents target selection for the copy.
 *
 * @property affectedPlayerId The player who is discarding
 * @property amount The damage amount for the copy
 * @property spellName The name of the spell being copied
 * @property sourceId The source entity of the original spell/ability
 */
@Serializable
data class DamageChainDiscardContinuation(
    override val decisionId: String,
    val affectedPlayerId: EntityId,
    val amount: Int,
    val spellName: String,
    val sourceId: EntityId?
) : ContinuationFrame

/**
 * Resume after the copying player selects a target for the damage chain copy.
 *
 * Creates a TriggeredAbilityOnStackComponent with DamageAndChainCopyEffect targeting
 * the selected entity, enabling recursive chaining.
 *
 * @property copyControllerId The player who is creating the copy
 * @property amount The damage amount for the copy
 * @property spellName The name of the spell being copied
 * @property sourceId The source entity of the original spell/ability
 * @property candidateTargets The list of valid target entity IDs (for validation)
 */
@Serializable
data class DamageChainCopyTargetContinuation(
    override val decisionId: String,
    val copyControllerId: EntityId,
    val amount: Int,
    val spellName: String,
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
 * Information about a mana source available for manual selection.
 */
@Serializable
data class ManaSourceOption(
    val entityId: EntityId,
    val name: String,
    val producesColors: Set<Color>,
    val producesColorless: Boolean
)
