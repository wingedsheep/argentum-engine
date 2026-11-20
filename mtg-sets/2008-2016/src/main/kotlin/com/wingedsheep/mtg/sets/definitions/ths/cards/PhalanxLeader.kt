package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Phalanx Leader — Theros #26 (canonical printing)
 * {W}{W}
 * Creature — Human Soldier
 * 1/1
 * Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on each
 * creature you control.
 *
 * Heroic is the `TargetsSource` cast trigger (Fabled Hero); the payoff is Cathars' Crusade's
 * ForEachInGroup counter spread.
 */
val PhalanxLeader = card("Phalanx Leader") {
    manaCost = "{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 1
    oracleText = "Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on each creature you control."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl()),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity)
        )
        description = "Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on each creature you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "26"
        artist = "David Palumbo"
        flavorText = "His soldiers etch his words on the insides of their shields, their inspiration always in sight during battle."
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c6c2458-edb7-4822-866b-ca1b2ea7a8e8.jpg?1783939810"
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
