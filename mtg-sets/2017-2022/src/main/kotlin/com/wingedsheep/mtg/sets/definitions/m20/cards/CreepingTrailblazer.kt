package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Creeping Trailblazer (M20 #207)
 * {R}{G}  Creature — Elemental  2/2
 *
 * Other Elementals you control get +1/+0.
 * {2}{R}{G}: This creature gets +1/+1 until end of turn for each Elemental you control.
 *
 * The pump's count is an `AggregateBattlefield` tally read as the ability resolves and locked in
 * for the turn (2019-07-12 ruling); the Trailblazer counts itself while it's on the battlefield.
 */
val CreepingTrailblazer = card("Creeping Trailblazer") {
    manaCost = "{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Elemental"
    oracleText = "Other Elementals you control get +1/+0.\n{2}{R}{G}: This creature gets +1/+1 until end of turn for each Elemental you control."
    power = 2
    toughness = 2

    // Other Elementals you control get +1/+0.
    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 0,
            filter = GroupFilter(
                GameObjectFilter.Permanent.withSubtype(Subtype.ELEMENTAL).youControl(),
                excludeSelf = true
            )
        )
    }

    // {2}{R}{G}: This creature gets +1/+1 until end of turn for each Elemental you control.
    activatedAbility {
        cost = Costs.Mana("{2}{R}{G}")
        val elementals = DynamicAmounts.battlefield(
            Player.You,
            GameObjectFilter.Permanent.withSubtype(Subtype.ELEMENTAL),
        ).count()
        effect = Effects.ModifyStats(elementals, elementals, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "207"
        artist = "Johan Grenier"
        flavorText = "The destruction of a wildfire with the tenacity of a jungle vine."
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0b96e41-caae-4e22-82f9-df5a054ea3b7.jpg?1783932951"
        ruling("2019-07-12", "The size of the bonus is determined as Creeping Trailblazer's last ability begins to resolve; it won't change later in the turn if the number of Elementals you control changes.")
        ruling("2019-07-12", "Because Creeping Trailblazer is itself an Elemental, its last ability will usually give it at least +1/+1.")
    }
}
