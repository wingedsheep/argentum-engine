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
 * Charging War Boar — Ravnica Allegiance #271
 * {1}{R}{G} · Creature — Boar · 3 / 1
 *
 * "A Domri planeswalker" is the planeswalker filter narrowed by the planeswalker *subtype*
 * (CR 205.3j); [Conditions.YouControl] is the battlefield-existence check. The pump and the
 * trample grant are **two** conditional statics sharing one condition rather than one
 * [ConditionalStaticAbility] over a composite — they live in different layers (7c and 6), so
 * keeping them apart is what lets the layer system order them independently.
 */
val ChargingWarBoar = card("Charging War Boar") {
    manaCost = "{1}{R}{G}"
    colorIdentity = "GR"
    typeLine = "Creature — Boar"
    power = 3
    toughness = 1
    oracleText = "Haste (This creature can attack and {T} as soon as it comes under your control.)\n" +
        "As long as you control a Domri planeswalker, this creature gets +1/+1 and has trample. (It can deal excess damage to the player or planeswalker it's attacking.)"

    keywords(Keyword.HASTE)
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(powerBonus = 1, toughnessBonus = 1, filter = GroupFilter.source()),
            condition = Conditions.YouControl(GameObjectFilter.Planeswalker.withSubtype("Domri"))
        )
    }
    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.TRAMPLE, GroupFilter.source()),
            condition = Conditions.YouControl(GameObjectFilter.Planeswalker.withSubtype("Domri"))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "271"
        inBooster = false
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02cef5a4-e8fd-4ebd-b121-67059308c772.jpg"
    }
}
