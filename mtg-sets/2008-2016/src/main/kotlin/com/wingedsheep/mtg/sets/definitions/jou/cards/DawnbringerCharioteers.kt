package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dawnbringer Charioteers — Journey into Nyx #6 (canonical printing)
 * {2}{W}{W}
 * Creature — Human Soldier
 * 2/4
 * Flying, lifelink
 * Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on this creature.
 *
 * Heroic is the `TargetsSource` cast trigger (Akroan Skyguard, Hero of the Nyxborn).
 */
val DawnbringerCharioteers = card("Dawnbringer Charioteers") {
    manaCost = "{2}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 4
    oracleText = "Flying, lifelink\nHeroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on this creature."

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        description = "Heroic — Whenever you cast a spell that targets this creature, put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "6"
        artist = "Ryan Alexander Lee"
        flavorText = "\"Nyx may belong to the gods, but the skies of Theros are ours.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/c/eca7b7d9-8b69-411a-8a1d-9b7d0492e7d0.jpg?1783939463"
        ruling("2014-04-26", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2014-04-26", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2014-04-26", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
