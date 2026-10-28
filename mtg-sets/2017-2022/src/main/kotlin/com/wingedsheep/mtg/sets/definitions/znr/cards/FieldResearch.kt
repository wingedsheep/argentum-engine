package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.WasKicked

/**
 * Field Research
 * {2}{U}
 * Sorcery
 * Kicker {2}{U}
 * Draw two cards. If this spell was kicked, draw three cards instead.
 */
val FieldResearch = card("Field Research") {
    manaCost = "{2}{U}"
    typeLine = "Sorcery"
    oracleText = "Kicker {2}{U} (You may pay an additional {2}{U} as you cast this spell.)\n" +
        "Draw two cards. If this spell was kicked, draw three cards instead."

    keywordAbility(KeywordAbility.kicker("{2}{U}"))

    spell {
        effect = Effects.If(
            condition = WasKicked,
            then = Effects.DrawCards(3),
            otherwise = Effects.DrawCards(2)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "58"
        artist = "PINDURSKI"
        flavorText = "\"In the field, speed-reading is a survival skill.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a48c3a2f-678b-4e70-a53f-258457d88d29.jpg?1783929398"
    }
}
