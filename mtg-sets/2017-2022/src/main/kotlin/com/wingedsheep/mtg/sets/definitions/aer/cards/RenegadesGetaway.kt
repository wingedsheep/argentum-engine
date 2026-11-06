package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Renegade's Getaway
 * {2}{B}
 * Instant
 * Target permanent gains indestructible until end of turn. Create a 1/1 colorless Servo artifact creature token.
 */
val RenegadesGetaway = card("Renegade's Getaway") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target permanent gains indestructible until end of turn. Create a 1/1 colorless Servo artifact " +
        "creature token. (Effects that say \"destroy\" don't destroy a permanent with indestructible, and if it's " +
        "a creature, it can't be destroyed by damage.)"

    spell {
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, permanent) then Effects.CreateToken(
            power = 1,
            toughness = 1,
            creatureTypes = setOf("Servo"),
            artifactToken = true
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "69"
        artist = "Ryan Yee"
        flavorText = "You can't fight what you can't find."
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49b0a5d5-99d7-492b-bd85-77c3cee12c8d.jpg"
        ruling("2017-02-09", "There's no way to have Renegade's Getaway's effect create a Servo token and give that token indestructible.")
        ruling("2017-02-09", "If the target permanent becomes an illegal target, Renegade's Getaway doesn't resolve and none of its effects happen. You won't create a Servo token.")
    }
}
