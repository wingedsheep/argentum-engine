package com.wingedsheep.gameserver.session

import com.wingedsheep.engine.core.*
import com.wingedsheep.engine.registry.CardRegistry
import com.wingedsheep.engine.state.GameState
import com.wingedsheep.engine.state.components.identity.CardComponent
import com.wingedsheep.engine.state.components.identity.FaceDownComponent
import com.wingedsheep.gameserver.protocol.ServerMessage
import com.wingedsheep.sdk.model.EntityId

class DecisionEnricher(private val cardRegistry: CardRegistry) {

    fun enrich(decision: PendingDecision, state: GameState): PendingDecision {
        return when (decision) {
            is SearchLibraryDecision -> decision.copy(
                cards = decision.cards.mapValues { (entityId, cardInfo) ->
                    val cardComponent = state.getEntity(entityId)?.get<CardComponent>()
                    val imageUri = cardComponent?.cardDefinitionId?.let { defId ->
                        cardRegistry.getCard(defId)?.metadata?.imageUri
                    }
                    cardInfo.copy(imageUri = imageUri)
                }
            )
            is ReorderLibraryDecision -> decision.copy(
                cardInfo = decision.cardInfo.mapValues { (entityId, cardInfo) ->
                    val cardComponent = state.getEntity(entityId)?.get<CardComponent>()
                    val imageUri = cardComponent?.cardDefinitionId?.let { defId ->
                        cardRegistry.getCard(defId)?.metadata?.imageUri
                    }
                    cardInfo.copy(imageUri = imageUri)
                }
            )
            is SelectCardsDecision -> {
                val enrichedCardInfo = decision.cardInfo?.mapValues { (entityId, cardInfo) ->
                    val cardComponent = state.getEntity(entityId)?.get<CardComponent>()
                    val imageUri = cardComponent?.cardDefinitionId?.let { defId ->
                        cardRegistry.getCard(defId)?.metadata?.imageUri
                    }
                    cardInfo.copy(imageUri = imageUri)
                }
                decision.copy(cardInfo = enrichedCardInfo)
            }
            is OrderObjectsDecision -> {
                val enrichedCardInfo = decision.cardInfo?.mapValues { (entityId, cardInfo) ->
                    val entity = state.getEntity(entityId)
                    // Don't enrich face-down creatures - would leak their identity
                    if (entity?.has<FaceDownComponent>() == true) {
                        cardInfo
                    } else {
                        val cardComponent = entity?.get<CardComponent>()
                        val imageUri = cardComponent?.cardDefinitionId?.let { defId ->
                            cardRegistry.getCard(defId)?.metadata?.imageUri
                        }
                        cardInfo.copy(imageUri = imageUri)
                    }
                }
                decision.copy(cardInfo = enrichedCardInfo)
            }
            is SplitPilesDecision -> {
                val enrichedCardInfo = decision.cardInfo?.mapValues { (entityId, cardInfo) ->
                    val cardComponent = state.getEntity(entityId)?.get<CardComponent>()
                    val imageUri = cardComponent?.cardDefinitionId?.let { defId ->
                        cardRegistry.getCard(defId)?.metadata?.imageUri
                    }
                    cardInfo.copy(imageUri = imageUri)
                }
                decision.copy(cardInfo = enrichedCardInfo)
            }
            // Other decision types don't have card info to enrich
            else -> decision
        }
    }

    fun createOpponentDecisionStatus(decision: PendingDecision): ServerMessage.OpponentDecisionStatus {
        val displayText = when (decision) {
            is SelectCardsDecision -> "Selecting cards"
            is ChooseTargetsDecision -> "Choosing targets"
            is YesNoDecision -> "Making a choice"
            is ChooseModeDecision -> "Choosing mode"
            is ChooseColorDecision -> "Choosing a color"
            is ChooseNumberDecision -> "Choosing a number"
            is DistributeDecision -> "Distributing"
            is OrderObjectsDecision -> "Ordering blockers"
            is SplitPilesDecision -> "Splitting piles"
            is SearchLibraryDecision -> "Searching library"
            is ReorderLibraryDecision -> "Reordering cards"
            is AssignDamageDecision -> "Assigning damage"
            is ChooseOptionDecision -> "Making a choice"
            is SelectManaSourcesDecision -> "Selecting mana sources"
        }
        return ServerMessage.OpponentDecisionStatus(
            decisionType = decision::class.simpleName ?: "Unknown",
            displayText = displayText,
            sourceName = decision.context.sourceName
        )
    }
}
