package com.wingedsheep.mtg.sets.definitions.fin

import com.wingedsheep.mtg.sets.definitions.fin.cards.*
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.MtgSet

/**
 * Final Fantasy (2025)
 *
 * Set Code: FIN
 * Release Date: June 13, 2025
 */
object FinalFantasySet : MtgSet {

    override val code = "FIN"
    override val displayName = "Final Fantasy"
    override val incomplete = true
    override val sealedSupported = true

    override val basicLands: List<CardDefinition> = emptyList()

    override val cards: List<CardDefinition> = listOf(
        SazhsChocobo,
        TravelingChocobo,
        TifaLockhart,
    )
}
