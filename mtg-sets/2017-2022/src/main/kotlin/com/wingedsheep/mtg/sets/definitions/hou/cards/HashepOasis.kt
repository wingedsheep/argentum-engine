package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hashep Oasis — Hour of Devastation #177 (canonical printing)
 * Land — Desert
 * {T}: Add {C}.
 * {T}, Pay 1 life: Add {G}.
 * {1}{G}{G}, {T}, Sacrifice a Desert: Target creature gets +3/+3 until end of turn. Activate only as a sorcery.
 *
 * "Sacrifice a Desert" is a plain filtered sacrifice cost (not `SacrificeAnother`), so Hashep Oasis
 * may sacrifice itself to pay for its own ability (the 2017-07-14 ruling).
 */
val HashepOasis = card("Hashep Oasis") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land — Desert"
    oracleText = "{T}: Add {C}.\n{T}, Pay 1 life: Add {G}.\n{1}{G}{G}, {T}, Sacrifice a Desert: Target creature gets +3/+3 until end of turn. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(1))
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{G}{G}"),
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Land.withSubtype("Desert")),
        )
        timing = TimingRule.SorcerySpeed
        val creature = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(3, 3, creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "177"
        artist = "Jonas De Ro"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3bba75d2-3f1b-4828-be11-2da695f7bde7.jpg?1783935996"
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
