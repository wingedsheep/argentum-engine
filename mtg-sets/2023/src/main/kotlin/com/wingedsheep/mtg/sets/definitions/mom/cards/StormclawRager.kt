package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Stormclaw Rager {1}{B}{R}
 * Creature — Ogre Warrior
 * 2/2
 * {1}, Sacrifice another creature or artifact: Put a +1/+1 counter on this creature and draw a
 * card. Activate only as a sorcery.
 */
val StormclawRager = card("Stormclaw Rager") {
    manaCost = "{1}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Creature — Ogre Warrior"
    power = 2
    toughness = 2
    oracleText = "{1}, Sacrifice another creature or artifact: Put a +1/+1 counter on this creature " +
        "and draw a card. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.SacrificeAnother(GameObjectFilter.Creature or GameObjectFilter.Artifact)
        )
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.DrawCards(1)
        timing = TimingRule.SorcerySpeed
        description = "{1}, Sacrifice another creature or artifact: Put a +1/+1 counter on this " +
            "creature and draw a card. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "254"
        artist = "Nicholas Elias"
        flavorText = "No amount of spilled oil and crushed metal could slake her bloodlust."
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b00c6e1-dfda-4c47-b1d8-4c114a921477.jpg?1783916938"
    }
}
