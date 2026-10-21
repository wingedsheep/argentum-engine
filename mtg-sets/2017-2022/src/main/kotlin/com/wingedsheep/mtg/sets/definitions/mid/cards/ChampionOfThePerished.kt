package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Champion of the Perished
 * {B}
 * Creature — Zombie
 * 1/1
 * Whenever another Zombie you control enters, put a +1/+1 counter on this creature.
 *
 * The bare noun "Zombie" means any Zombie permanent, so the trigger filter is
 * [GameObjectFilter.Permanent]; "another" is the OTHER binding.
 */
val ChampionOfThePerished = card("Champion of the Perished") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    oracleText = "Whenever another Zombie you control enters, put a +1/+1 counter on this creature."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Permanent.withSubtype(Subtype.ZOMBIE).youControl()).enters()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "91"
        artist = "Kekai Kotaki"
        flavorText = "He rose from the graf for every cobbler, tanner, and fool who'd been slaughtered in the parish—and they rose and shambled after him."
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d8e6941-ad6e-4d2f-93c0-be79e75bdf06.jpg"
    }
}
