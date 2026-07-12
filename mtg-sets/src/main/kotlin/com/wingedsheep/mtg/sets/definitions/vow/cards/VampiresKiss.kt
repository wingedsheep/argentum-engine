package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Vampire's Kiss
 * {1}{B}
 * Sorcery
 *
 * Target player loses 2 life and you gain 2 life. Create two Blood tokens.
 */
val VampiresKiss = card("Vampire's Kiss") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target player loses 2 life and you gain 2 life. Create two Blood tokens. (They're " +
        "artifacts with \"{1}, {T}, Discard a card, Sacrifice this token: Draw a card.\")"

    spell {
        val t = target("target", TargetPlayer())
        effect = Effects.Composite(
            Effects.LoseLife(2, t),
            Effects.GainLife(2, EffectTarget.Controller),
            Effects.CreateBlood(2)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "136"
        artist = "Ilse Gort"
        flavorText = "Young vampires gorge themselves at every meal, but their elders have learned to savor the smallest bite."
        imageUri = "https://cards.scryfall.io/normal/front/9/7/974bf8cc-4259-48cc-8e7f-1580bb010d3f.jpg?1782703092"
    }
}
