package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Undercellar Myconid
 * {2}{G}
 * Creature — Fungus
 * 1/2
 * Whenever this creature enters or dies, create a 1/1 green Saproling creature token.
 * {T}: Add one mana of any color.
 *
 * "Enters or dies" is one ability with two trigger events; no single event is both, so it is
 * written as an enters trigger and a dies trigger sharing the same token effect.
 */
val UndercellarMyconid = card("Undercellar Myconid") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Fungus"
    power = 1
    toughness = 2
    oracleText = "Whenever this creature enters or dies, create a 1/1 green Saproling creature token.\n" +
        "{T}: Add one mana of any color."

    val saproling = Effects.CreateToken(
        power = 1,
        toughness = 1,
        colors = setOf(Color.GREEN),
        creatureTypes = setOf("Saproling"),
        imageUri = "https://cards.scryfall.io/normal/front/f/7/f7cbc9fc-42d2-4d1a-b6c0-29a35cfd1588.jpg?1783922319"
    )

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = saproling
        description = "Whenever this creature enters, create a 1/1 green Saproling creature token."
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = saproling
        description = "Whenever this creature dies, create a 1/1 green Saproling creature token."
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "259"
        artist = "David Szabo"
        flavorText = "Water, barley, yeast, darkness—the tavern cellar had everything a growing mold could need."
        imageUri = "https://cards.scryfall.io/normal/front/7/8/786c5b3e-737f-4794-b482-af5f0ee901e5.jpg?1783922702"
    }
}
