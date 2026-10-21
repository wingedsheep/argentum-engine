package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bull's Strength
 * {1}{G}
 * Instant
 * Target creature gets +2/+2 and gains trample until end of turn. Untap it.
 */
val BullsStrength = card("Bull's Strength") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 and gains trample until end of turn. Untap it."
    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 2, t) then Effects.GrantKeyword(Keyword.TRAMPLE, t) then Effects.Untap(t)
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "174"
        artist = "Gabor Szikszai"
        flavorText = "As the iron bars of the portcullis parted like reeds, the defenders of the keep suddenly felt much less secure."
        imageUri = "https://cards.scryfall.io/normal/front/0/9/092cab70-c771-4c96-a5ce-22f2e32e3ea8.jpg?1783926467"
    }
}
