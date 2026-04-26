package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shimmercreep
 * {4}{B}
 * Creature — Elemental
 * 3/5
 *
 * Menace
 * Vivid — When this creature enters, each opponent loses X life and you gain X life,
 * where X is the number of colors among permanents you control.
 */
val Shimmercreep = card("Shimmercreep") {
    manaCost = "{4}{B}"
    typeLine = "Creature — Elemental"
    oracleText = "Menace (This creature can't be blocked except by two or more creatures.)\n" +
        "Vivid — When this creature enters, each opponent loses X life and you gain X life, " +
        "where X is the number of colors among permanents you control."
    power = 3
    toughness = 5

    keywords(Keyword.MENACE)

    vividEtb { colorCount ->
        CompositeEffect(listOf(
            Effects.LoseLife(colorCount, EffectTarget.PlayerRef(Player.EachOpponent)),
            Effects.GainLife(colorCount, EffectTarget.Controller)
        ))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "120"
        artist = "Nils Hamm"
        flavorText = "It lurks near the borders, harvesting loose threads of wild magic where the planes meet."
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7c02899-5f0f-4b38-bbbc-fbc8c46419a6.jpg?1767732711"
    }
}
