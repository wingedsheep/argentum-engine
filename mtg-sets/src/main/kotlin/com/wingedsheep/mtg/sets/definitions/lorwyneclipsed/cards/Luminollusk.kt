package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.GainLifeEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Luminollusk
 * {3}{G}
 * Creature — Elemental
 * 2/4
 *
 * Deathtouch
 * Vivid — When this creature enters, you gain life equal to the number of colors among
 * permanents you control.
 */
val Luminollusk = card("Luminollusk") {
    manaCost = "{3}{G}"
    typeLine = "Creature — Elemental"
    oracleText = "Deathtouch\nVivid — When this creature enters, you gain life equal to the number of " +
        "colors among permanents you control."
    power = 2
    toughness = 4

    keywords(Keyword.DEATHTOUCH)

    vividEtb { colorCount ->
        GainLifeEffect(amount = colorCount, target = EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "179"
        artist = "Maxime Minard"
        flavorText = "Its trail of wild magic attracts boggart aunties searching for the rarest " +
            "ingredients in Shadowmoor."
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d591e66-4bf9-47e7-bcef-57769ec3edc6.jpg?1767872025"
    }
}
