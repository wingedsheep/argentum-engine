package com.wingedsheep.rulesengine.zone

import com.wingedsheep.rulesengine.card.CardInstance
import com.wingedsheep.rulesengine.core.CardId
import kotlinx.serialization.Serializable

@Serializable
data class Zone(
    val type: ZoneType,
    val cards: List<CardInstance> = emptyList(),
    val ownerId: String? = null
) {
    val size: Int get() = cards.size
    val isEmpty: Boolean get() = cards.isEmpty()
    val isNotEmpty: Boolean get() = cards.isNotEmpty()

    fun contains(cardId: CardId): Boolean = cards.any { it.id == cardId }

    fun getCard(cardId: CardId): CardInstance? = cards.find { it.id == cardId }

    fun topCard(): CardInstance? = cards.lastOrNull()

    fun bottomCard(): CardInstance? = cards.firstOrNull()

    fun addToTop(card: CardInstance): Zone = copy(cards = cards + card)

    fun addToBottom(card: CardInstance): Zone = copy(cards = listOf(card) + cards)

    fun addAt(index: Int, card: CardInstance): Zone {
        val mutableCards = cards.toMutableList()
        mutableCards.add(index.coerceIn(0, cards.size), card)
        return copy(cards = mutableCards)
    }

    fun addAll(newCards: List<CardInstance>): Zone = copy(cards = cards + newCards)

    fun remove(cardId: CardId): Zone = copy(cards = cards.filter { it.id != cardId })

    fun removeTop(): Pair<CardInstance?, Zone> {
        return if (cards.isNotEmpty()) {
            cards.last() to copy(cards = cards.dropLast(1))
        } else {
            null to this
        }
    }

    fun removeBottom(): Pair<CardInstance?, Zone> {
        return if (cards.isNotEmpty()) {
            cards.first() to copy(cards = cards.drop(1))
        } else {
            null to this
        }
    }

    fun updateCard(cardId: CardId, transform: (CardInstance) -> CardInstance): Zone =
        copy(cards = cards.map { if (it.id == cardId) transform(it) else it })

    fun shuffle(): Zone = copy(cards = cards.shuffled())

    fun shuffle(random: java.util.Random): Zone = copy(cards = cards.shuffled(random))

    fun shuffle(random: kotlin.random.Random): Zone = copy(cards = cards.shuffled(random))

    fun filter(predicate: (CardInstance) -> Boolean): List<CardInstance> = cards.filter(predicate)

    fun findAll(predicate: (CardInstance) -> Boolean): List<CardInstance> = cards.filter(predicate)

    companion object {
        fun library(ownerId: String, cards: List<CardInstance> = emptyList()): Zone =
            Zone(ZoneType.LIBRARY, cards, ownerId)

        fun hand(ownerId: String, cards: List<CardInstance> = emptyList()): Zone =
            Zone(ZoneType.HAND, cards, ownerId)

        fun graveyard(ownerId: String, cards: List<CardInstance> = emptyList()): Zone =
            Zone(ZoneType.GRAVEYARD, cards, ownerId)

        fun battlefield(cards: List<CardInstance> = emptyList()): Zone =
            Zone(ZoneType.BATTLEFIELD, cards, null)

        fun stack(cards: List<CardInstance> = emptyList()): Zone =
            Zone(ZoneType.STACK, cards, null)

        fun exile(cards: List<CardInstance> = emptyList()): Zone =
            Zone(ZoneType.EXILE, cards, null)

        fun command(cards: List<CardInstance> = emptyList()): Zone =
            Zone(ZoneType.COMMAND, cards, null)
    }
}
