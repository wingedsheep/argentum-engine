package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Foul-Tongue Shriek
 * {B}
 * Instant
 *
 * Target opponent loses 1 life for each attacking creature you control. You gain that much life.
 *
 * The attacker count is read at resolution, and [Effects.DrainLife] gains exactly the life the
 * opponent actually lost (none with no attackers; less if the loss is replaced or reduced).
 */
val FoulTongueShriek = card("Foul-Tongue Shriek") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target opponent loses 1 life for each attacking creature you control. " +
        "You gain that much life."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.DrainLife(
            DynamicAmounts.attackingCreaturesYouControl(),
            from = opponent,
            to = EffectTarget.Controller
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "103"
        artist = "Dave Kendall"
        flavorText = "Foul-Tongue shamans draw their powers from a dark twisting of the Draconic language."
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81d98ee0-6b32-4735-89f1-b37da766761f.jpg?1783938597"
    }
}
