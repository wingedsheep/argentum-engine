package com.wingedsheep.mtg.sets.definitions.mat

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.limited.BoosterStrategy
import com.wingedsheep.sdk.limited.PlayBooster
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing
import com.wingedsheep.sdk.model.Rarity

/**
 * March of the Machine: The Aftermath (2023)
 *
 * A 50-card epilogue set to March of the Machine, sold in its own Set and Collector Boosters.
 * Scaffolded to hold the canonical definitions of cards it printed first. It is an extension
 * set — too thin to carry a limited pool on its own.
 *
 * Set Code: MAT
 * Release Date: May 12, 2023
 */
object MarchOfTheMachineTheAftermathSet : MtgSet {

    override val code = "MAT"
    override val displayName = "March of the Machine: The Aftermath"
    override val releaseDate = "2023-05-12"
    override val extensionSet = true

    /**
     * The 5-card Epilogue Booster (per WotC's "Collecting March of the Machine: The Aftermath"):
     * two uncommons, a rare or mythic rare, a traditional-foil card of any rarity and a Booster Fun
     * card of any rarity — each of those two a rare or mythic in about 1 booster in 6 — plus an ad
     * card or token (not modelled). The set has no commons, so a wildcard is otherwise an uncommon.
     * Foil and Booster Fun treatments aren't modelled; the rare/mythic split inside a slot isn't
     * published, so it keeps the house 7:1.
     */
    override val boosterStrategy: BoosterStrategy = PlayBooster(
        commons = 0,
        uncommons = 2,
        raresOrMythics = 1,
        wildcards = 2,
        wildcardWeights = linkedMapOf(
            Rarity.UNCOMMON to 40.0,
            Rarity.RARE to 7.0,
            Rarity.MYTHIC to 1.0,
        ),
    )

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.mat.cards"
}
