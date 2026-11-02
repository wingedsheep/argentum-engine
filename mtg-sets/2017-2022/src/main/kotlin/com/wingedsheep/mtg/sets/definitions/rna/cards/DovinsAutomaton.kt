package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Dovin's Automaton — Ravnica Allegiance #268
 * {4} · Artifact Creature — Homunculus · 3 / 3
 *
 * The colourless twin of [ChargingWarBoar]'s shape — two conditional statics over one
 * "you control a Dovin planeswalker" condition, kept apart so layers 7c and 6 order
 * independently.
 */
val DovinsAutomaton = card("Dovin's Automaton") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Homunculus"
    power = 3
    toughness = 3
    oracleText = "As long as you control a Dovin planeswalker, this creature gets +2/+2 and has vigilance. (Attacking doesn't cause it to tap.)"

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(powerBonus = 2, toughnessBonus = 2, filter = GroupFilter.source()),
            condition = Conditions.YouControl(GameObjectFilter.Planeswalker.withSubtype("Dovin"))
        )
    }
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.VIGILANCE, GroupFilter.source()),
            condition = Conditions.YouControl(GameObjectFilter.Planeswalker.withSubtype("Dovin"))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "268"
        inBooster = false
        artist = "Adam Paquette"
        flavorText = "It was made for battle, but that doesn't mean it's unsophisticated."
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a962509-2e77-4655-b397-9625b2f3407a.jpg"
    }
}
