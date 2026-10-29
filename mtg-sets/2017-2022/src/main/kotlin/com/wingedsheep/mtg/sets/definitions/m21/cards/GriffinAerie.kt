package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Griffin Aerie
 * {1}{W}
 * Enchantment
 * At the beginning of your end step, if you gained 3 or more life this turn, create a 2/2 white
 * Griffin creature token with flying.
 *
 * Intervening-if on [Conditions.YouGainedLifeThisTurnAtLeast] — the turn's total life gained,
 * regardless of life lost or whether the Aerie was on the battlefield then (rulings).
 */
val GriffinAerie = card("Griffin Aerie") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your end step, if you gained 3 or more life this turn, create " +
        "a 2/2 white Griffin creature token with flying."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.YouGainedLifeThisTurnAtLeast(3)
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Griffin"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/e/b/ebbaae25-d0cd-416a-a44a-5b258fd1d9fd.jpg?1783930593"
        )
        description = "At the beginning of your end step, if you gained 3 or more life this turn, " +
            "create a 2/2 white Griffin creature token with flying."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "22"
        artist = "Milivoj Ćeran"
        flavorText = "When griffins started nesting atop the northern tower, the castellan worried " +
            "they'd be a nuisance. Instead, they took the entire castle under their protection."
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6ea1ee60-5644-4f78-913d-32c36065957f.jpg?1783930740"
        ruling("2020-06-23", "You create just one Griffin token, no matter how much life you've gained past 3 life.")
        ruling("2020-06-23", "Griffin Aerie's ability looks at how much life you've gained in the turn, even if it wasn't on the battlefield when you gained life. It doesn't care if you also lost life, even if you lost more life than you gained.")
        ruling("2020-06-23", "If you haven't gained 3 life by the time your end step begins, Griffin Aerie's ability won't trigger at all.")
    }
}
