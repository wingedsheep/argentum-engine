package com.wingedsheep.mtg.sets.definitions.ddp

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Duel Decks: Zendikar vs. Eldrazi (2015)
 *
 * Scaffolded so that the cards this product printed *first* can hold their canonical
 * definitions. Retreat to Kazandu, Sheer Drop and Dominator Drone are such cards: the duel deck
 * shipped five weeks before Battle for Zendikar, so DDP — not BFZ — is their earliest real printing.
 *
 * The rest of the deck is reprints, not all authored here yet, hence [incomplete].
 *
 * [sealedSupported] stays false: a duel deck is two fixed 60-card decks, not a draftable product.
 *
 * Set Code: DDP
 * Release Date: 2015-08-28
 */
object DuelDecksZendikarVsEldraziSet : MtgSet {

    override val code = "DDP"
    override val displayName = "Duel Decks: Zendikar vs. Eldrazi"
    override val releaseDate = "2015-08-28"
    override val sealedSupported = false
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.ddp.cards"
}
