package com.wingedsheep.engine.core

import com.wingedsheep.sdk.model.EntityId
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import kotlinx.serialization.Serializable

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
    val storedCollections: Map<String, List<EntityId>> = emptyMap(),
    /**
     * Restrictions that tightened the selection bounds. The resumer uses these
     * to normalize the player's response: e.g. [SelectionRestriction.OnePerCardType]
     * drops any extra cards that share a card type with an already-kept selection,
     * routing them into the remainder collection.
     */
    val restrictions: List<SelectionRestriction> = emptyList()
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
 * Resume after a card's owner chooses one of the offered library positions.
 *
 * Used by PutOnLibraryPositionOfChoiceEffect: the owner has chosen where to put
 * the card. The continuation moves the card to the chosen position. The
 * [positions] list mirrors [options] one-to-one and is the canonical source for
 * mapping a chosen index to its [LibraryChoicePosition].
 *
 * @property ownerId The owner making the choice
 * @property cardId The card being put into the library
 * @property sourceId The spell/ability that caused this effect
 * @property sourceName Name of the source for event messages
 * @property options The option strings shown to the player
 * @property positions The library positions corresponding to each option, by index
 */
@Serializable
data class PutOnTopOrBottomContinuation(
    override val decisionId: String,
    val ownerId: EntityId,
    val cardId: EntityId,
    val sourceId: EntityId?,
    val sourceName: String?,
    val options: List<String>,
    val positions: List<com.wingedsheep.sdk.scripting.effects.LibraryChoicePosition> = emptyList()
) : ContinuationFrame

/**
 * Resume after player chooses a card to return from a linked exile.
 *
 * Used for effects like Dimensional Breach's upkeep trigger: the active player
 * chooses one of their owned exiled cards to return to the battlefield.
 * The exiled cards are tracked via LinkedExileComponent on the source entity.
 *
 * @property playerId The player making the choice
 * @property sourceId The entity whose LinkedExileComponent tracks the exiled cards
 * @property eligibleCards All cards this player could choose from
 */
@Serializable
data class ReturnFromLinkedExileContinuation(
    override val decisionId: String,
    val playerId: EntityId,
    val sourceId: EntityId,
    val eligibleCards: List<EntityId>
) : ContinuationFrame
