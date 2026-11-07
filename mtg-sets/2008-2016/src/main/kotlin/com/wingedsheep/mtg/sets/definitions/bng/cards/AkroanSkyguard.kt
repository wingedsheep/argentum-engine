package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Akroan Skyguard — Born of the Gods #3 (canonical printing)
 * {1}{W}
 * Creature — Human Soldier
 * 1/1
 * Flying
 * Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on this creature.
 *
 * Heroic is the `TargetsSource` cast trigger (Hero of the Nyxborn, Tenth District Legionnaire).
 */
val AkroanSkyguard = card("Akroan Skyguard") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 1
    oracleText = "Flying\nHeroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on this creature."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "3"
        artist = "Mark Winters"
        flavorText = "\"Trust me. When you have earned a god's favor, you'll know.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/5/d55e85c3-234e-49c4-a307-96fb4497eea8.jpg?1783939586"
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
