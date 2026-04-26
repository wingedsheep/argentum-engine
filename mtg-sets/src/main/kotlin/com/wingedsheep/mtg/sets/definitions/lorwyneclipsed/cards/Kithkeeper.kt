package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kithkeeper
 * {6}{W}
 * Creature — Elemental
 * 3/3
 *
 * Vivid — When this creature enters, create X 1/1 green and white Kithkin creature
 * tokens, where X is the number of colors among permanents you control.
 * Tap three untapped creatures you control: This creature gets +3/+0 and gains flying
 * until end of turn.
 */
val Kithkeeper = card("Kithkeeper") {
    manaCost = "{6}{W}"
    typeLine = "Creature — Elemental"
    oracleText = "Vivid — When this creature enters, create X 1/1 green and white Kithkin creature " +
        "tokens, where X is the number of colors among permanents you control.\n" +
        "Tap three untapped creatures you control: This creature gets +3/+0 and gains flying " +
        "until end of turn."
    power = 3
    toughness = 3

    vividEtb { colorCount ->
        CreateTokenEffect(
            count = colorCount,
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN, Color.WHITE),
            creatureTypes = setOf("Kithkin"),
            imageUri = "https://cards.scryfall.io/normal/front/2/e/2ed11e1b-2289-48d2-8d96-ee7e590ecfd4.jpg?1767955680"
        )
    }

    activatedAbility {
        cost = Costs.TapPermanents(3, GameObjectFilter.Creature)
        effect = CompositeEffect(
            listOf(
                Effects.ModifyStats(3, 0, EffectTarget.Self),
                Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/ef29eff7-72be-46e6-9275-0f0a44d29233.jpg?1767871704"
        ruling("2025-11-17", "The value of X is calculated only once, as Kithkeeper's first ability resolves.")
    }
}
