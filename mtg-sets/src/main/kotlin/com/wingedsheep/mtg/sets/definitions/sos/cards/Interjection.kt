package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Interjection
 * {W}
 * Instant
 * Target creature gets +2/+2 and gains first strike until end of turn.
 */
val Interjection = card("Interjection") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 and gains first strike until end of turn."
    spell {
        val t = target("target creature", Targets.Creature)
        effect = Effects.ModifyStats(2, 2, t)
            .then(Effects.GrantKeyword(Keyword.FIRST_STRIKE, t))
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "22"
        artist = "Anna Pavleeva"
        flavorText = "Silverquill professors dislike when students disrupt their lessons, but can't help approving of the confidence needed to do so."
        imageUri = "https://cards.scryfall.io/normal/front/0/5/0534cff6-299c-4155-b318-eb7581989e8a.jpg"
    }
}
