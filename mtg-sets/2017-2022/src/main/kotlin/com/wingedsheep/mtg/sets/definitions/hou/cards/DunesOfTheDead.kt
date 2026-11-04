package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Dunes of the Dead
 * Land — Desert
 * {T}: Add {C}.
 * When this land is put into a graveyard from the battlefield, create a 2/2 black Zombie creature token.
 *
 * "Put into a graveyard from the battlefield" is the same battlefield → graveyard zone change
 * [Triggers.self]`.dies()` matches.
 */
val DunesOfTheDead = card("Dunes of the Dead") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Land — Desert"
    oracleText = "{T}: Add {C}.\nWhen this land is put into a graveyard from the battlefield, create a 2/2 black Zombie creature token."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie"),
            imageUri = "https://cards.scryfall.io/normal/front/b/5/b5bd6905-79be-4d2c-a343-f6e6a181b3e6.jpg?1783936411"
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "175"
        artist = "E. M. Gist"
        flavorText = "The Curse of Wandering permeated Amonkhet long before Nicol Bolas. He simply harnessed it."
        imageUri = "https://cards.scryfall.io/normal/front/3/2/326c7483-2c45-4804-85c4-d40ecdba4fc7.jpg?1783935996"
    }
}
