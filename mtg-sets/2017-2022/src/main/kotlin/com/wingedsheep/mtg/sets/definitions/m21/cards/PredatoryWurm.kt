package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats

/**
 * Predatory Wurm — Core Set 2021 #338 (Garruk planeswalker deck)
 * {3}{G} · Creature — Wurm · 4/4
 *
 * Vigilance
 * This creature gets +2/+2 as long as you control a Garruk planeswalker.
 *
 * The Renegade Firebrand shape: a self [ModifyStats] gated on controlling a Garruk planeswalker.
 */
val PredatoryWurm = card("Predatory Wurm") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wurm"
    power = 4
    toughness = 4
    oracleText = "Vigilance (Attacking doesn't cause this creature to tap.)\n" +
        "This creature gets +2/+2 as long as you control a Garruk planeswalker."

    keywords(Keyword.VIGILANCE)

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(2, 2, Filters.Self),
            condition = Conditions.YouControl(GameObjectFilter.Planeswalker.withSubtype("Garruk")),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "338"
        artist = "Jason A. Engle"
        flavorText = "\"The strong eat. The weak are eaten. That's the way of things.\"\n—Garruk Wildspeaker"
        imageUri = "https://cards.scryfall.io/normal/front/4/0/4073cbc6-11de-4d4a-bcd7-792f9b7c5219.jpg?1783930615"
        ruling("2020-06-23", "Because damage remains marked on a creature until the damage is removed as the turn ends, nonlethal damage dealt to Predatory Wurm may become lethal if you controlled a Garruk planeswalker and he leaves the battlefield during that turn.")
    }
}
