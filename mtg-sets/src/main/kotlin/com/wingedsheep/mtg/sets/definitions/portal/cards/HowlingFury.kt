package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.ModifyStatsEffect
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Howling Fury
 * {2}{B}
 * Sorcery
 * Target creature gets +4/+0 until end of turn.
 */
val HowlingFury = card("Howling Fury") {
    manaCost = "{2}{B}"
    typeLine = "Sorcery"

    spell {
        val t = target("target", TargetCreature())
        effect = ModifyStatsEffect(
            powerModifier = 4,
            toughnessModifier = 0,
            target = t,
            duration = Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "97"
        artist = "Clyde Caldwell"
        flavorText = "The rage of the cursed can be harnessed but never controlled."
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a49a7c61-8696-4bab-9c96-05028db3a9f9.jpg"
    }
}
