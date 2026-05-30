package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect

/**
 * Roar of Endless Song
 * {2}{G}{U}{R}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I, II — Create a 5/5 green Elephant creature token.
 * III — Double the power and toughness of each creature you control until end of turn.
 *
 * Chapter III composes the reusable [EffectPatterns.doublePowerAndToughnessForAll] helper
 * (shared with Unnatural Growth) — no card-specific doubling logic.
 */
val RoarOfEndlessSong = card("Roar of Endless Song") {
    manaCost = "{2}{G}{U}{R}"
    colorIdentity = "GUR"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I, II — Create a 5/5 green Elephant creature token.\n" +
        "III — Double the power and toughness of each creature you control until end of turn."

    sagaChapter(1) {
        effect = CreateTokenEffect(
            power = 5,
            toughness = 5,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Elephant"),
            name = "Elephant",
            imageUri = "https://cards.scryfall.io/normal/front/2/4/243bcfa9-0310-4d68-9864-df46069906fa.jpg?1743176747"
        )
    }

    sagaChapter(2) {
        effect = CreateTokenEffect(
            power = 5,
            toughness = 5,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Elephant"),
            name = "Elephant",
            imageUri = "https://cards.scryfall.io/normal/front/2/4/243bcfa9-0310-4d68-9864-df46069906fa.jpg?1743176747"
        )
    }

    sagaChapter(3) {
        effect = EffectPatterns.doublePowerAndToughnessForAll(Filters.Group.creaturesYouControl)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "220"
        artist = "Clint Lockwood"
        imageUri = "https://cards.scryfall.io/normal/front/7/a/7a9c3531-61a8-43f5-82a2-5166e5f5a6b9.jpg?1744577948"
    }
}
