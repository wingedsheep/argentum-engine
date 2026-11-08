package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Woodcutter's Grit (Eldritch Moon #179)
 * {2}{G}
 * Instant
 *
 * Target creature you control gets +3/+3 and gains hexproof until end of turn.
 */
val WoodcuttersGrit = card("Woodcutter's Grit") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature you control gets +3/+3 and gains hexproof until end of turn. " +
        "(It can't be the target of spells or abilities your opponents control.)"

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.ModifyStats(3, 3, creature) then Effects.GrantKeyword(Keyword.HEXPROOF, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "179"
        artist = "Anthony Palumbo"
        flavorText = "The magic used by Kessigers is known for its practical applications."
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc329134-4b71-4c33-8cfb-c3867a733115.jpg"
    }
}
