package com.wingedsheep.mtg.sets.definitions.lrw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

val TurtleshellChangeling = card("Turtleshell Changeling") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Shapeshifter"
    power = 1
    toughness = 4
    oracleText = "Changeling (This card is every creature type.)\n{1}{U}: Switch this creature's power and toughness until end of turn."
    keywords(Keyword.CHANGELING)

    activatedAbility {
        cost = Costs.Mana("{1}{U}")
        effect = Effects.SwitchPowerToughness(EffectTarget.Self)
        description = "Switch this creature's power and toughness until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "94"
        artist = "Ron Spencer"
        flavorText = "A changeling involuntarily mimics the nearest being at hand, sometimes trading a borrowed shell for borrowed claws."
        imageUri = "https://cards.scryfall.io/normal/front/a/7/a72b078e-e324-4775-9d38-08943017a48e.jpg?1783942895"
        ruling("2021-03-19", "Effects that switch a creature's power and toughness apply after all other effects, regardless of when those effects began to apply. For instance, if you target a 1/2 creature then give it +2/+0 later in the turn, it's a 2/3 creature, not a 4/1 creature.")
        ruling("2021-03-19", "Because damage remains marked on a creature until the damage is removed as the turn ends, nonlethal damage dealt to a creature may become lethal if you switch its power and toughness during that turn.")
        ruling("2021-03-19", "Switching a creature's power and toughness twice (or any even number of times) effectively returns the creature to the power and toughness it had before any switches.")
    }
}
