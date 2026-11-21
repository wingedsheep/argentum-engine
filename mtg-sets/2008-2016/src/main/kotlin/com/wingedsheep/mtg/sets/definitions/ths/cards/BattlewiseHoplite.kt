package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Battlewise Hoplite — Theros #189 (canonical printing)
 * {W}{U}
 * Creature — Human Soldier
 * 2/2
 * Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on this creature,
 * then scry 1.
 */
val BattlewiseHoplite = card("Battlewise Hoplite") {
    manaCost = "{W}{U}"
    colorIdentity = "WU"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on this creature, then scry 1. (To scry 1, look at the top card of your library, then you may put that card on the bottom.)"

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then Effects.Scry(1)
        description = "Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on this creature, then scry 1."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "189"
        artist = "Willian Murai"
        imageUri = "https://cards.scryfall.io/normal/front/4/c/4c929a57-2c23-4bbf-b265-666630a4fde8.jpg?1783939731"
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
