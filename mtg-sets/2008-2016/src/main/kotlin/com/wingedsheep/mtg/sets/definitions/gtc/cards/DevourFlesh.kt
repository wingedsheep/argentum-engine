package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Devour Flesh
 * {1}{B}
 * Instant
 * Target player sacrifices a creature of their choice, then gains life equal to that creature's
 * toughness.
 *
 * Tribute to Hunger's edict-then-gain shape, except the life goes to the *targeted* player rather
 * than the caster: the edict snapshots the sacrificed creature into the effect context, and
 * [Effects.GainLife] reads [DynamicAmounts.sacrificedToughness] for it, aimed at the same target. If
 * the player controls no creatures, nothing is sacrificed and no life is gained.
 */
val DevourFlesh = card("Devour Flesh") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target player sacrifices a creature of their choice, " +
        "then gains life equal to that creature's toughness."

    spell {
        val t = target(Targets.Player)
        effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, t) then
            Effects.GainLife(DynamicAmounts.sacrificedToughness(0), t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "63"
        artist = "Kev Walker"
        flavorText = "His twisted mind concluded that if he was what he ate, and he wanted to stay human, . . ."
        imageUri = "https://cards.scryfall.io/normal/front/8/8/88c42ebd-114a-430d-b3a4-ff2fb3093bf5.jpg?1783940131"
    }
}
