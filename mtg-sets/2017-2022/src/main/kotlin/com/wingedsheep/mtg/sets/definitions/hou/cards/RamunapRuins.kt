package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ramunap Ruins — Hour of Devastation #181 (canonical printing)
 * Land — Desert
 * {T}: Add {C}.
 * {T}, Pay 1 life: Add {R}.
 * {2}{R}{R}, {T}, Sacrifice a Desert: This land deals 2 damage to each opponent.
 *
 * "Sacrifice a Desert" is a plain filtered sacrifice cost (not `SacrificeAnother`), so Ramunap Ruins
 * may sacrifice itself to pay for its own ability (the 2017-07-14 ruling).
 */
val RamunapRuins = card("Ramunap Ruins") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land — Desert"
    oracleText = "{T}: Add {C}.\n{T}, Pay 1 life: Add {R}.\n{2}{R}{R}, {T}, Sacrifice a Desert: This land deals 2 damage to each opponent."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(1))
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{R}{R}"),
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Land.withSubtype("Desert")),
        )
        effect = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "181"
        artist = "Florian de Gesincourt"
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af11d41a-0d29-45e9-9d27-a41282b9e292.jpg?1783935995"
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
