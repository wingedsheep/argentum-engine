package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shinestriker
 * {4}{U}{U}
 * Creature — Elemental
 * 3/3
 *
 * Flying
 * Vivid — When this creature enters, draw cards equal to the number of colors among
 * permanents you control.
 */
val Shinestriker = card("Shinestriker") {
    manaCost = "{4}{U}{U}"
    typeLine = "Creature — Elemental"
    oracleText = "Flying\nVivid — When this creature enters, draw cards equal to the number of colors " +
        "among permanents you control."
    power = 3
    toughness = 3

    keywords(Keyword.FLYING)

    vividEtb { colorCount ->
        DrawCardsEffect(count = colorCount, target = EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "68"
        artist = "Ron Spencer"
        flavorText = "Its spines are highly prized among faeries for use as lances, " +
            "but they're almost impossible to harvest."
        imageUri = "https://cards.scryfall.io/normal/front/2/1/214e78f9-5364-49ab-b17e-0c76549c583e.jpg?1767659586"
    }
}
