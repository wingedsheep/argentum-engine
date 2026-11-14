package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Malakir Cullblade
 * {1}{B}
 * Creature — Vampire Warrior
 * 1/1
 * Whenever a creature an opponent controls dies, put a +1/+1 counter on this creature.
 */
val MalakirCullblade = card("Malakir Cullblade") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Vampire Warrior"
    power = 1
    toughness = 1
    oracleText = "Whenever a creature an opponent controls dies, put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.opponentControls()).dies()
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "108"
        artist = "Igor Kieryluk"
        flavorText = "Each cut stains his blade with fresh blood that he samples to increase his strength."
        imageUri = "https://cards.scryfall.io/normal/front/7/0/70861692-1eb2-4cb1-8d7f-b881386c37d1.jpg?1783938339"
    }
}
