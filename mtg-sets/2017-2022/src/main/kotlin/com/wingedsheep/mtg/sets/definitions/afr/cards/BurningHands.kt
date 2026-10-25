package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Burning Hands
 * {1}{R}
 * Instant
 * Burning Hands deals 2 damage to target creature or planeswalker. If that permanent is green,
 * Burning Hands deals 6 damage instead.
 *
 * The colour is checked as the spell resolves, against projected state.
 */
val BurningHands = card("Burning Hands") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Burning Hands deals 2 damage to target creature or planeswalker. If that permanent is green, " +
        "Burning Hands deals 6 damage instead."

    spell {
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Any.withColor(Color.GREEN), t),
            then = Effects.DealDamage(6, t),
            otherwise = Effects.DealDamage(2, t)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "135"
        artist = "Olena Richards"
        flavorText = "A simple fire spell that grows in intensity along with its caster."
        imageUri = "https://cards.scryfall.io/normal/front/6/6/66e2d723-3fa0-4411-8f98-e4e6b3a5e6df.jpg?1783926483"
    }
}
