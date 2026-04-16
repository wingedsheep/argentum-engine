package com.wingedsheep.ai.llm

import com.wingedsheep.sdk.model.EntityId

data class MulliganInfo(
    val hand: List<EntityId>,
    val mulliganCount: Int,
    val cardsToPutOnBottom: Int,
    val cards: Map<EntityId, CardSummary> = emptyMap(),
    val isOnThePlay: Boolean = false
)

data class BottomCardsInfo(
    val hand: List<EntityId>,
    val cardsToPutOnBottom: Int,
    val cards: Map<EntityId, CardSummary> = emptyMap()
)

data class CardSummary(
    val name: String,
    val manaCost: String? = null,
    val typeLine: String? = null,
    val rarity: String? = null,
    val imageUri: String? = null,
    val power: Int? = null,
    val toughness: Int? = null,
    val oracleText: String? = null,
    val rulings: List<CardRuling> = emptyList()
)

data class CardRuling(val date: String, val text: String)
