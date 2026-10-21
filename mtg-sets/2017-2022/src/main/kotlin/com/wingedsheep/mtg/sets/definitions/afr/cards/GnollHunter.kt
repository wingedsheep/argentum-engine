package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gnoll Hunter
 * {1}{G}
 * Creature — Gnoll
 * 2/2
 * Pack tactics — Whenever this creature attacks, if you attacked with creatures with total power 6
 * or greater this combat, put a +1/+1 counter on this creature.
 *
 * Pack tactics is the attack trigger gated by [AttackPredicate.AttackersTotalPowerAtLeast], read
 * once as attackers are declared.
 */
val GnollHunter = card("Gnoll Hunter") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Gnoll"
    oracleText = "Pack tactics — Whenever this creature attacks, if you attacked with creatures with " +
        "total power 6 or greater this combat, put a +1/+1 counter on this creature."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.AttackersTotalPowerAtLeast(6)))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "185"
        artist = "Jesper Ejsing"
        flavorText = "Yipping and cackling, the hunters tore through the village, leaving nothing but snapped bones and burning homes in their wake."
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d0634cb-e973-4f05-965e-d39b09fe57a9.jpg?1783926462"
    }
}
