package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hellion Crucible — Magic 2013 #226 (canonical printing)
 * Land
 *
 * {T}: Add {C}.
 * {1}{R}, {T}: Put a pressure counter on this land.
 * {1}{R}, {T}, Remove two pressure counters from this land and sacrifice it: Create a 4/4 red
 * Hellion creature token with haste.
 */
val HellionCrucible = card("Hellion Crucible") {
    manaCost = ""
    typeLine = "Land"
    oracleText = "{T}: Add {C}.\n" +
        "{1}{R}, {T}: Put a pressure counter on this land.\n" +
        "{1}{R}, {T}, Remove two pressure counters from this land and sacrifice it: Create a 4/4 red " +
        "Hellion creature token with haste. (It can attack and {T} as soon as it comes under your control.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{R}"), Costs.Tap)
        effect = Effects.AddCounters(CounterType.PRESSURE, 1, EffectTarget.Self)
        description = "{1}{R}, {T}: Put a pressure counter on this land."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{R}"),
            Costs.Tap,
            Costs.RemoveCounterFromSelf(CounterType.PRESSURE, 2),
            Costs.SacrificeSelf,
        )
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Hellion"),
            keywords = setOf(Keyword.HASTE),
        )
        description = "{1}{R}, {T}, Remove two pressure counters from this land and sacrifice it: " +
            "Create a 4/4 red Hellion creature token with haste."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "226"
        artist = "Trevor Claxton"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/ad8274ef-a46a-4f5f-8ad1-6ce828f24210.jpg?1783940453"
    }
}
