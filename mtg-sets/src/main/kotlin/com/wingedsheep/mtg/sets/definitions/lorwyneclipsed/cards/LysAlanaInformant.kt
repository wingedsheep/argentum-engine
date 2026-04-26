package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Lys Alana Informant
 * {1}{G}
 * Creature — Elf Scout
 * 3/1
 *
 * When this creature enters or dies, surveil 1.
 */
val LysAlanaInformant = card("Lys Alana Informant") {
    manaCost = "{1}{G}"
    typeLine = "Creature — Elf Scout"
    power = 3
    toughness = 1
    oracleText = "When this creature enters or dies, surveil 1. " +
        "(Look at the top card of your library. You may put it into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = EffectPatterns.surveil(1)
    }

    triggeredAbility {
        trigger = Triggers.Dies
        effect = EffectPatterns.surveil(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "181"
        artist = "Sidharth Chaturvedi"
        flavorText = "While they are no longer the dominant power on Lorwyn, " +
            "elves still revel in looking down on others."
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a79649c4-559e-4306-a102-5fd8750629c7.jpg?1767658380"
    }
}
