package com.wingedsheep.gameserver.session

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.sdk.model.EntityId

class DecisionEnricher(private val cardRegistry: CardRegistry) {

    private companion object {
        /** Generic label for a face-down (morph / manifest) creature; mirrors the client state transformer. */
        const val FACE_DOWN_CREATURE_NAME = "Face-down creature"
    }

    /**
     * Whether [entityId]'s real name must be hidden from [viewerId]. A face-down permanent's identity
     * is known only to the player who controls it (they may look at their own face-down creatures);
     * everyone else sees the generic label. Mirrors the battlefield masking in ClientStateTransformer
     * (`isFaceDown && controllerId != viewingPlayerId`). It intentionally does not honour
     * Lens-of-Clarity-style reveals — omitting them only ever over-masks, so it can't leak.
     */
    private fun isHiddenFrom(state: GameState, entityId: EntityId, viewerId: EntityId): Boolean =
        state.getEntity(entityId)?.has<FaceDownComponent>() == true &&
            state.projectedState.getController(entityId) != viewerId

    /**
     * The source name to display for [decision] to [viewerId]. The combat board copies the (single)
     * attacker's real name into [DecisionContext.sourceName]; mask it when the viewer isn't its
     * controller. The multi-attacker board already uses a generic "Combat damage" label.
     */
    /**
     * The art to display for [entityId].
     *
     * Reads the entity's own [CardComponent.imageUri], which `CardEntityFactory` stamps from the
     * printing the player actually put in their deck. Re-deriving it from the canonical
     * [com.wingedsheep.sdk.model.CardDefinition] metadata instead (as this used to) shows the
     * *original* printing's art for every reprint, so a card in a search/reveal prompt didn't match
     * the same card in hand or on the battlefield — both of which read the component. The definition
     * lookup remains only as a fallback for entities with no image stamped.
     */
    private fun imageUriFor(state: GameState, entityId: EntityId): String? {
        val cardComponent = state.getEntity(entityId)?.get<CardComponent>() ?: return null
        return cardComponent.imageUri
            ?: cardRegistry.getCard(cardComponent.cardDefinitionId)?.metadata?.imageUri
    }

    private fun maskedSourceName(decision: PendingDecision, state: GameState, viewerId: EntityId): String? {
        val sourceName = decision.context.sourceName ?: return null
        if (decision is CombatResolutionDecision) {
            val single = decision.attackers.singleOrNull() ?: return sourceName
            if (single.name == sourceName && isHiddenFrom(state, single.id, viewerId)) return FACE_DOWN_CREATURE_NAME
        }
        return sourceName
    }

    fun enrich(decision: PendingDecision, state: GameState, viewerId: EntityId): PendingDecision {
        return when (decision) {
            is SearchLibraryDecision -> decision.copy(
                cards = decision.cards.mapValues { (entityId, cardInfo) ->
                    cardInfo.copy(imageUri = imageUriFor(state, entityId))
                }
            )
            is ReorderLibraryDecision -> decision.copy(
                cardInfo = decision.cardInfo.mapValues { (entityId, cardInfo) ->
                    cardInfo.copy(imageUri = imageUriFor(state, entityId))
                }
            )
            is SelectCardsDecision -> decision.copy(
                cardInfo = decision.cardInfo?.mapValues { (entityId, cardInfo) ->
                    cardInfo.copy(imageUri = imageUriFor(state, entityId))
                }
            )
            is OrderObjectsDecision -> decision.copy(
                cardInfo = decision.cardInfo?.mapValues { (entityId, cardInfo) ->
                    // Don't enrich face-down creatures - would leak their identity
                    if (state.getEntity(entityId)?.has<FaceDownComponent>() == true) cardInfo
                    else cardInfo.copy(imageUri = imageUriFor(state, entityId))
                }
            )
            is SplitPilesDecision -> decision.copy(
                cardInfo = decision.cardInfo?.mapValues { (entityId, cardInfo) ->
                    cardInfo.copy(imageUri = imageUriFor(state, entityId))
                }
            )
            is CombatResolutionDecision -> {
                // The combat-damage board is shown to every chooser (the attacker assigns its damage,
                // the defender assigns any blocker damage), so a face-down creature's real name would
                // leak to the opponent through a shared node. Mask per viewer: the controller keeps
                // its own creature's name, everyone else sees the generic label.
                val maskedAttackers = decision.attackers.map {
                    if (isHiddenFrom(state, it.id, viewerId)) it.copy(name = FACE_DOWN_CREATURE_NAME) else it
                }
                val maskedBlockers = decision.blockers.map {
                    if (isHiddenFrom(state, it.id, viewerId)) it.copy(name = FACE_DOWN_CREATURE_NAME) else it
                }
                // The single-attacker prompt embeds that attacker's name; mask it in lockstep.
                val single = decision.attackers.singleOrNull()
                val maskedPrompt = if (single != null && isHiddenFrom(state, single.id, viewerId)) {
                    decision.prompt.replaceFirst(single.name, FACE_DOWN_CREATURE_NAME)
                } else {
                    decision.prompt
                }
                decision.copy(
                    attackers = maskedAttackers,
                    blockers = maskedBlockers,
                    prompt = maskedPrompt,
                    context = decision.context.copy(sourceName = maskedSourceName(decision, state, viewerId)),
                )
            }
            // Other decision types don't have card info to enrich
            else -> decision
        }
    }

    fun createOpponentDecisionStatus(
        decision: PendingDecision,
        state: GameState,
        viewerId: EntityId,
    ): ServerMessage.OpponentDecisionStatus {
        val displayText = when (decision) {
            is SelectCardsDecision -> "Selecting cards"
            is ChooseTargetsDecision -> "Choosing targets"
            is YesNoDecision -> "Making a choice"
            is BatchYesNoDecision -> "Making a choice"
            is ChooseModeDecision -> "Choosing mode"
            is ChooseColorDecision -> "Choosing a color"
            is ChooseNumberDecision -> "Choosing a number"
            is DistributeDecision -> "Distributing"
            is OrderObjectsDecision -> "Ordering blockers"
            is SplitPilesDecision -> "Splitting piles"
            is SearchLibraryDecision -> "Searching library"
            is ReorderLibraryDecision -> "Reordering cards"
            is AssignDamageDecision -> "Assigning damage"
            is CombatResolutionDecision -> "Assigning combat damage"
            is ChooseOptionDecision -> "Making a choice"
            is ChooseReplacementDecision -> "Changing text"
            is BudgetModalDecision -> "Choosing modes"
            is SelectManaSourcesDecision -> "Selecting mana sources"
        }
        return ServerMessage.OpponentDecisionStatus(
            playerId = decision.playerId.value,
            decisionType = decision::class.simpleName ?: "Unknown",
            displayText = displayText,
            sourceName = maskedSourceName(decision, state, viewerId)
        )
    }
}
