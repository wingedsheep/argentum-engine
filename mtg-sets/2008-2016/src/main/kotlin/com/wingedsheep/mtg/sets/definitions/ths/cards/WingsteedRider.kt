package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wingsteed Rider — Theros #36 (canonical printing)
 * {1}{W}{W}
 * Creature — Human Knight
 * 2/2
 * Flying
 * Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on this creature.
 *
 * Heroic is the `TargetsSource` cast trigger (Akroan Skyguard, Hero of the Nyxborn).
 */
val WingsteedRider = card("Wingsteed Rider") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Knight"
    power = 2
    toughness = 2
    oracleText = "Flying\nHeroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on this creature."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "36"
        artist = "Cynthia Sheppard"
        flavorText = "Trust is the only bridle a pegasus will accept."
        imageUri = "https://cards.scryfall.io/normal/front/e/0/e04751f2-b571-49fa-94b6-ec589d314ee6.jpg?1783939804"
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
