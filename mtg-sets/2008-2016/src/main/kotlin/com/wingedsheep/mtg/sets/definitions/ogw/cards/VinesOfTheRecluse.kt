package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Vines of the Recluse
 * {G}
 * Instant
 * Target creature gets +1/+2 and gains reach until end of turn. Untap it. (A creature with reach
 * can block creatures with flying.)
 */
val VinesOfTheRecluse = card("Vines of the Recluse") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature gets +1/+2 and gains reach until end of turn. Untap it. " +
        "(A creature with reach can block creatures with flying.)"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(1, 2, creature) then
            Effects.GrantKeyword(Keyword.REACH, creature) then
            Effects.Untap(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "146"
        artist = "Dan Murayama Scott"
        flavorText = "\"The most elegant solutions are found in nature. You just have to know where to look.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e0a1fc4b-ec68-4fd1-af0f-6ccc8c6dee89.jpg"
    }
}
