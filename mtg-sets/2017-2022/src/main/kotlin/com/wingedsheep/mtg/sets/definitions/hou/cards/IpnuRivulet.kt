package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Ipnu Rivulet — Hour of Devastation #180 (canonical printing)
 * Land — Desert
 * {T}: Add {C}.
 * {T}, Pay 1 life: Add {U}.
 * {1}{U}, {T}, Sacrifice a Desert: Target player mills four cards.
 *
 * Unlike the rest of the cycle, the sacrifice ability has no sorcery-speed restriction.
 * "Sacrifice a Desert" is a plain filtered sacrifice cost (not `SacrificeAnother`), so Ipnu Rivulet
 * may sacrifice itself to pay for its own ability (the 2017-07-14 ruling).
 */
val IpnuRivulet = card("Ipnu Rivulet") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land — Desert"
    oracleText = "{T}: Add {C}.\n{T}, Pay 1 life: Add {U}.\n{1}{U}, {T}, Sacrifice a Desert: Target player mills four cards. (They put the top four cards of their library into their graveyard.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.PayLife(1))
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{U}"),
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Land.withSubtype("Desert")),
        )
        val player = target(Targets.Player)
        effect = Patterns.Library.mill(4, player)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "180"
        artist = "James Paick"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/203011ef-3737-4fd1-bd23-0e531b5a7c32.jpg?1783935996"
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
