package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Yew Spirit — Avacyn Restored #207 (canonical printing)
 * {4}{G} · Creature — Spirit Treefolk · 3/3
 *
 * {2}{G}{G}: This creature gets +X/+X until end of turn, where X is its power.
 *
 * Mirrors [com.wingedsheep.mtg.sets.definitions.kld.cards.ElectrostaticPummeler]: X is read once as
 * the ability resolves, and negative power counts as 0 — [DynamicAmounts.nonNegative] over the
 * source's projected power.
 */
val YewSpirit = card("Yew Spirit") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Spirit Treefolk"
    oracleText = "{2}{G}{G}: This creature gets +X/+X until end of turn, where X is its power."
    power = 3
    toughness = 3

    activatedAbility {
        cost = Costs.Mana("{2}{G}{G}")
        val x = DynamicAmounts.nonNegative(DynamicAmounts.sourcePower())
        effect = Effects.ModifyStats(x, x, EffectTarget.Self)
        description = "{2}{G}{G}: This creature gets +X/+X until end of turn, where X is its power."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "207"
        artist = "Dan Murayama Scott"
        flavorText = "\"I wonder what dwelled in the primordial forests before humans existed.\"\n—Elder Rimheit"
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9320432-4f89-4363-91e6-2e740535cc2e.jpg?1783940656"
        ruling("2012-05-01", "The value of X is determined when the ability resolves. The bonus won't change later in the turn if the creature's power changes.")
        ruling("2017-07-14", "If this creature's power is negative as its ability resolves, X is considered to be 0.")
    }
}
