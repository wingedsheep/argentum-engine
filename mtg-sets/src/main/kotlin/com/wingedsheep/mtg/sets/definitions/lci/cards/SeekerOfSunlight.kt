package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Seeker of Sunlight — {G}
 * Creature — Merfolk Scout
 * 1/1
 * {2}{G}: This creature explores. Activate only as a sorcery.
 */
val SeekerOfSunlight = card("Seeker of Sunlight") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Merfolk Scout"
    oracleText = "{2}{G}: This creature explores. Activate only as a sorcery. (Reveal the top card of your library. Put that card into your hand if it's a land. Otherwise, put a +1/+1 counter on this creature, then put the card back or put it into your graveyard.)"
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        timing = TimingRule.SorcerySpeed
        effect = Effects.Explore(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "210"
        artist = "Randy Vargas"
        flavorText = "\"We weren't fleeing a broken world. We were running toward paradise!\""
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ffcdb10f-4bad-4f89-8e7c-daa5c0c69230.jpg?1782694440"
    }
}
