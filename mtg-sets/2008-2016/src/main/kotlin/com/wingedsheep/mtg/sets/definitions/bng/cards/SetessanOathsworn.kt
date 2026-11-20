package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Setessan Oathsworn — Born of the Gods #138 (canonical printing)
 * {1}{G}{G}
 * Creature — Satyr Warrior
 * 1/1
 * Heroic — Whenever you cast a spell that targets this creature, put two +1/+1 counters on this creature.
 */
val SetessanOathsworn = card("Setessan Oathsworn") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Satyr Warrior"
    power = 1
    toughness = 1
    oracleText = "Heroic — Whenever you cast a spell that targets this creature, put two +1/+1 counters on this creature."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.Self)
        description = "Heroic — Whenever you cast a spell that targets this creature, put two +1/+1 counters on this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "138"
        artist = "Scott Murphy"
        flavorText = "\"Setessa is not the city of my birth, but it is the place I fight for, and the place I'm willing to die for. Does that not make it my home?\""
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e5579755-4718-406a-bba6-27f1f2811e59.jpg?1783939529"
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
