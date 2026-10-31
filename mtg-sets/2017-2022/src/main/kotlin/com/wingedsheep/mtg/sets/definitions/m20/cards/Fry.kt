package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fry
 * {1}{R}
 * Instant
 *
 * This spell can't be countered.
 * Fry deals 5 damage to target creature or planeswalker that's white or blue.
 *
 * The red member of Core Set 2020's hoser cycle (see Devout Decree): the target is a single
 * creature-or-planeswalker restricted to white or blue via
 * [GameObjectFilter.CreatureOrPlaneswalker] + `withAnyColor(WHITE, BLUE)`.
 */
val Fry = card("Fry") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "This spell can't be countered.\n" +
        "Fry deals 5 damage to target creature or planeswalker that's white or blue."

    cantBeCountered = true

    spell {
        val victim = target(TargetFilter(GameObjectFilter.CreatureOrPlaneswalker.withAnyColor(Color.WHITE, Color.BLUE)))
        effect = Effects.DealDamage(5, victim)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "140"
        artist = "Tomasz Jedruszek"
        flavorText = "\"I'm afraid your dinner got a little . . . overcooked.\"\n—Marlan Farthwald, innkeeper"
        imageUri = "https://cards.scryfall.io/normal/front/0/7/070d7766-a662-443c-93c5-811dbdac2480.jpg?1783932978"
        ruling("2019-07-12", "A spell or ability that counters spells can still target Fry. When that spell or ability resolves, Fry won't be countered, but any additional effects of that spell or ability will still happen.")
        ruling("2019-07-12", "If the target creature or planeswalker is an illegal target by the time Fry tries to resolve, the spell won't resolve and will be put into its owner's graveyard with no effect. This is true even though it can't be countered.")
    }
}
