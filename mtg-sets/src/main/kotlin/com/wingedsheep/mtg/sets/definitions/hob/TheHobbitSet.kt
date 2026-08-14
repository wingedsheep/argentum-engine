package com.wingedsheep.mtg.sets.definitions.hob

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.TokenPrinting

/**
 * The Hobbit (2026)
 *
 * Set Code: HOB
 * Release Date: August 14, 2026
 * Preview inventory is sourced from Scryfall and may grow before release.
 */
object TheHobbitSet : MtgSet {
    override val code = "HOB"
    override val displayName = "The Hobbit"
    override val releaseDate = "2026-08-14"
    override val sealedSupported = false

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val basicLands: List<CardDefinition> by lazy {
        CardDiscovery.findBasicLandsIn(CARDS_PACKAGE, code)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    /**
     * The set is too new to appear in the bulk `tokens.json` sync, so the tokens its cards actually
     * mint are hand-authored here. Drop a row once `just token-art-sync` picks the same art up.
     */
    override val tokenArt: List<TokenPrinting> = listOf(
        // thob #12 — the Treasure minted by Long-Bodied Grey Dog and Dori, Bearer of Friends.
        TokenPrinting(
            name = "Treasure",
            imageUri = "https://cards.scryfall.io/art_crop/front/c/6/c6e096bb-ad9e-4a8b-8b42-26852fa32c1d.jpg?1783902770",
        ),
        // thob #3 — the Army that every "amass Goblins" card in the set puts its counters on
        // (Down, Down to Goblin Town, Gathering of Darkness, Rage into the Valley, Clap! Snap!).
        TokenPrinting(
            name = "Goblin Army",
            imageUri = "https://cards.scryfall.io/normal/front/2/e/2e2028b1-34c0-40b6-8f65-79f79a279996.jpg?1785497644",
        ),
        // thob #2 — the Soldier every recruit card in the set mints.
        TokenPrinting(
            name = "Human Soldier",
            imageUri = "https://cards.scryfall.io/art_crop/front/6/0/6007af81-4541-4b55-90ea-03d365362ae5.jpg?1785497653",
        ),
    )

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.hob.cards"
}
