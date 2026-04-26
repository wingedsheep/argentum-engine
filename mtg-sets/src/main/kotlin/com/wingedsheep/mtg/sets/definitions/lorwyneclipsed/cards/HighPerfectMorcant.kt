package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.references.Player

/**
 * High Perfect Morcant
 * {2}{B}{G}
 * Legendary Creature — Elf Noble
 * 4/4
 *
 * Whenever High Perfect Morcant or another Elf you control enters, each opponent
 * blights 1. (They each put a -1/-1 counter on a creature they control.)
 *
 * Tap three untapped Elves you control: Proliferate. Activate only as a sorcery.
 * (Choose any number of permanents and/or players, then give each another counter
 * of each kind already there.)
 */
val HighPerfectMorcant = card("High Perfect Morcant") {
    manaCost = "{2}{B}{G}"
    typeLine = "Legendary Creature — Elf Noble"
    power = 4
    toughness = 4
    oracleText =
        "Whenever High Perfect Morcant or another Elf you control enters, each opponent blights 1. (They each put a -1/-1 counter on a creature they control.)\n" +
        "Tap three untapped Elves you control: Proliferate. Activate only as a sorcery. (Choose any number of permanents and/or players, then give each another counter of each kind already there.)"

    triggeredAbility {
        // "this or another Elf you control enters" — ANY binding with an Elf-you-control filter
        // fires for both High Perfect Morcant itself and any other Elf you control.
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.withSubtype(Subtype.ELF).youControl(),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.ANY
        )
        // Each opponent blights 1: ForEachPlayerEffect rebinds Player.You to the
        // iterating opponent, so the stock blight pipeline picks one of THEIR creatures.
        effect = ForEachPlayerEffect(
            players = Player.EachOpponent,
            effects = EffectPatterns.blight(1).effects
        )
    }

    activatedAbility {
        cost = Costs.TapPermanents(3, GameObjectFilter.Creature.withSubtype(Subtype.ELF))
        effect = Effects.Proliferate()
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "229"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfe7b8bf-c150-4be0-aef4-e8bb6f09787a.jpg?1765451570"
    }
}
