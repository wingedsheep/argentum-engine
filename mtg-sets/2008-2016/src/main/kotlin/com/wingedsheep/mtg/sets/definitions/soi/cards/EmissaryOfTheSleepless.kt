package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Emissary of the Sleepless (Shadows over Innistrad #17)
 * {4}{W}
 * Creature — Spirit
 * 2/4
 *
 * Flying
 * When this creature enters, if a creature died this turn, create a 1/1 white Spirit creature token
 * with flying.
 */
val EmissaryOfTheSleepless = card("Emissary of the Sleepless") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Spirit"
    power = 2
    toughness = 4
    oracleText = "Flying\n" +
        "When this creature enters, if a creature died this turn, create a 1/1 white Spirit creature " +
        "token with flying."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.CreatureDiedThisTurn
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Spirit"),
            keywords = setOf(Keyword.FLYING),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc5fa496-a830-4031-a19a-d6467d074ad1.jpg"
    }
}
