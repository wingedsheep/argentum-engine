package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Furious Resistance
 * {R}
 * Instant
 * Target blocking creature gets +3/+0 and gains first strike until end of turn.
 */
val FuriousResistance = card("Furious Resistance") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Target blocking creature gets +3/+0 and gains first strike until end of turn."

    spell {
        val t = target(TargetFilter.BlockingCreature)
        effect = Effects.ModifyStats(3, 0, t) then Effects.GrantKeyword(Keyword.FIRST_STRIKE, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "93"
        artist = "Slawomir Maniak"
        flavorText = "\"At least the Gateless rebels have ideals. Rakdos just mocks, defiles, and kills all we hold dear.\"\n—Libuse, Boros sergeant"
        imageUri = "https://cards.scryfall.io/normal/front/4/e/4eeea013-1cf8-4c77-a097-aa69e141e3f4.jpg"
    }
}
