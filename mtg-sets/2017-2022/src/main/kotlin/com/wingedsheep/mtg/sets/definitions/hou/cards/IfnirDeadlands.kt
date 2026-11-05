package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Ifnir Deadlands — Hour of Devastation #179 (canonical printing)
 * Land — Desert
 * {T}: Add {C}.
 * {T}, Pay 1 life: Add {B}.
 * {2}{B}{B}, {T}, Sacrifice a Desert: Put two -1/-1 counters on target creature an opponent controls.
 * Activate only as a sorcery.
 *
 * "Sacrifice a Desert" is a plain filtered sacrifice cost (not `SacrificeAnother`), so Ifnir Deadlands
 * may sacrifice itself to pay for its own ability (the 2017-07-14 ruling).
 */
val IfnirDeadlands = card("Ifnir Deadlands") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land — Desert"
    oracleText = "{T}: Add {C}.\n{T}, Pay 1 life: Add {B}.\n{2}{B}{B}, {T}, Sacrifice a Desert: Put two -1/-1 counters on target creature an opponent controls. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(1))
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{B}{B}"),
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Land.withSubtype("Desert")),
        )
        timing = TimingRule.SorcerySpeed
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 2, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "179"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b88728b-9b18-40c6-b634-f87f8da83665.jpg?1783935996"
        ruling(
            "2017-07-14",
            "If a Desert has an ability with a cost of \"Sacrifice a Desert,\" you can sacrifice that Desert " +
                "to pay the cost for its own ability."
        )
        ruling(
            "2017-04-18",
            "Desert is a land subtype with no special meaning. It doesn't grant the land an intrinsic mana " +
                "ability. Other cards may care about which lands are Deserts."
        )
    }
}
