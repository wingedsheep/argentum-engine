package com.wingedsheep.mtg.sets.definitions.gpt

import com.wingedsheep.mtg.sets.discovery.CardDiscovery
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet
import com.wingedsheep.sdk.model.Printing

/**
 * Guildpact (2006)
 *
 * Set Code: GPT
 * Release Date: February 3, 2006
 */
object GuildpactSet : MtgSet {

    override val code = "GPT"
    override val displayName = "Guildpact"
    override val releaseDate = "2006-02-03"
    override val block = "Ravnica"
    override val basicLandsFallbackCode = "POR"
    override val incomplete = true

    override val cards: List<CardDefinition> by lazy {
        CardDiscovery.findIn(CARDS_PACKAGE)
    }

    override val printings: List<Printing> by lazy {
        CardDiscovery.findPrintingsIn(CARDS_PACKAGE)
    }

    private const val CARDS_PACKAGE = "com.wingedsheep.mtg.sets.definitions.gpt.cards"
}
