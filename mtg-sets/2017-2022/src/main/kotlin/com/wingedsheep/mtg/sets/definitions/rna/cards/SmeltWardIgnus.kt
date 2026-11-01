package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Smelt-Ward Ignus — Ravnica Allegiance #116
 * {1}{R} · Creature — Elemental · 2 / 1
 *
 * A sacrifice-outlet Threaten: the same gain-control / untap / haste chain as Claim the
 * Firstborn, gated on power rather than mana value, with sorcery timing on the ability.
 */
val SmeltWardIgnus = card("Smelt-Ward Ignus") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    power = 2
    toughness = 1
    oracleText = "{2}{R}, Sacrifice this creature: Gain control of target creature with power 3 or less until end of turn. Untap that creature. It gains haste until end of turn. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{R}"), Costs.SacrificeSelf)
        val t = target(TargetFilter.Creature.powerAtMost(3))
        effect = Effects.GainControl(t, Duration.EndOfTurn) then
            Effects.Untap(t) then
            Effects.GrantKeyword(Keyword.HASTE, t)
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "116"
        artist = "Jehan Choo"
        flavorText = "\"Come in, yes? Is warm!\""
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4f24b2b6-b994-455f-b8e6-aa73e1be81b4.jpg?1783933675"
    }
}
