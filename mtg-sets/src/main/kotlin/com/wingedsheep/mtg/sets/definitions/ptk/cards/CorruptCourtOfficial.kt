package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Corrupt Court Official
 * {1}{B}
 * Creature — Human Advisor
 * 1/1
 * When this creature enters, target opponent discards a card.
 *
 * Canonical printing: Portal Three Kingdoms (PTK, 1999) — the earliest real printing.
 * Later printings (SNC, Avatar: The Last Airbender) add only a
 * [com.wingedsheep.sdk.model.Printing] row.
 */
val CorruptCourtOfficial = card("Corrupt Court Official") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Advisor"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, target opponent discards a card."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val opponent = target("target opponent", Targets.Opponent)
        effect = Patterns.Hand.discardCards(1, opponent)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "71"
        artist = "Li Yousong"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d3ba2e3-e680-47cd-81c5-555deea7d00f.jpg?1562257498"
    }
}
