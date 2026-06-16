package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Ornery Tumblewagg
 * {2}{G}
 * Creature — Brushwagg Mount
 * 2/2
 *
 * At the beginning of combat on your turn, put a +1/+1 counter on target creature.
 * Whenever this creature attacks while saddled, double the number of +1/+1 counters on target
 * creature.
 * Saddle 2 (Tap any number of other creatures you control with total power 2 or more: This Mount
 * becomes saddled until end of turn. Saddle only as a sorcery.)
 *
 * "While saddled" is an intervening-if (CR 603.4) on the attack trigger. Each ability picks its
 * own target creature at the time it goes on the stack.
 */
val OrneryTumblewagg = card("Ornery Tumblewagg") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Brushwagg Mount"
    power = 2
    toughness = 2
    oracleText = "At the beginning of combat on your turn, put a +1/+1 counter on target creature.\n" +
        "Whenever this creature attacks while saddled, double the number of +1/+1 counters on " +
        "target creature.\n" +
        "Saddle 2 (Tap any number of other creatures you control with total power 2 or more: This " +
        "Mount becomes saddled until end of turn. Saddle only as a sorcery.)"

    keywordAbility(KeywordAbility.saddle(2))

    triggeredAbility {
        trigger = Triggers.BeginCombat
        val creature = target("target creature", TargetCreature())
        effect = Effects.AddCounters("+1/+1", 1, creature)
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        triggerCondition = Conditions.SourceIsSaddled
        val creature = target("target creature", TargetCreature())
        effect = Effects.DoubleCounters("+1/+1", creature)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "171"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/0/0/0020c31b-002a-4121-bc61-2c2c16e9afc8.jpg?1712355953"
    }
}
