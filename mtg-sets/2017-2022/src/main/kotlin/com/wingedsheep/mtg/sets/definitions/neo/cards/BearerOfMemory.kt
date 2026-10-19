package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bearer of Memory — Kamigawa: Neon Dynasty #174 (canonical printing)
 * {2}{G} · Enchantment Creature — Human Monk · 3/2
 *
 * {5}{G}: Put a +1/+1 counter on target enchantment creature. It gains trample until end of turn.
 */
val BearerOfMemory = card("Bearer of Memory") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Human Monk"
    power = 3
    toughness = 2
    oracleText = "{5}{G}: Put a +1/+1 counter on target enchantment creature. It gains trample until end of turn."

    activatedAbility {
        cost = Costs.Mana("{5}{G}")
        val creature = target(TargetFilter(GameObjectFilter.Creature and GameObjectFilter.Enchantment))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.GrantKeyword(Keyword.TRAMPLE, creature)
        description = "{5}{G}: Put a +1/+1 counter on target enchantment creature. It gains trample " +
            "until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "174"
        artist = "Randy Vargas"
        flavorText = "Members of the Living Historians don't simply chronicle the past; they embody it."
        imageUri = "https://cards.scryfall.io/normal/front/5/4/540ba70a-bea8-4512-83ba-92dab6d25b52.jpg?1783923854"
    }
}
