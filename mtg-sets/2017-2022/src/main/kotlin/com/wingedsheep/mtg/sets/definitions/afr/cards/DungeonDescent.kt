package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Dungeon Descent
 * Land
 * This land enters tapped.
 * {T}: Add {C}.
 * {4}, {T}, Tap an untapped legendary creature you control: Venture into the dungeon.
 * Activate only as a sorcery.
 */
val DungeonDescent = card("Dungeon Descent") {
    typeLine = "Land"
    oracleText = "This land enters tapped.\n" +
        "{T}: Add {C}.\n" +
        "{4}, {T}, Tap an untapped legendary creature you control: Venture into the dungeon. " +
        "Activate only as a sorcery. (Enter the first room or advance to the next room.)"

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    // "Tap an untapped legendary creature you control" is not the {T} symbol, so a
    // summoning-sick legendary creature can pay it.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{4}"),
            Costs.Tap,
            Costs.TapPermanents(1, GameObjectFilter.Creature.legendary())
        )
        effect = Effects.VentureIntoTheDungeon()
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "255"
        artist = "Kasia 'Kafis' Zielińska"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f4cccdbc-f4f4-42b6-9747-6ef703ff949a.jpg?1783926435"
        ruling("2021-07-23", "The player venturing into the dungeon chooses which dungeon they will venture into. They may choose a dungeon that they have already completed this game.")
        ruling("2021-07-23", "Choosing the dungeon or room to venture into is part of resolving the venture into the dungeon keyword action. Once that choice is made, players may not respond until after the appropriate room ability has triggered.")
    }
}
