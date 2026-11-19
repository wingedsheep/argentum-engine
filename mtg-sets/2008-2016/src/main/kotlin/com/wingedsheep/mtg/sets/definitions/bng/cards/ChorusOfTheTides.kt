package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Chorus of the Tides — Born of the Gods #33
 * {3}{U}
 * Creature — Siren
 * 3/2
 * Flying
 * Heroic — Whenever you cast a spell that targets this creature, scry 1.
 */
val ChorusOfTheTides = card("Chorus of the Tides") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Siren"
    power = 3
    toughness = 2
    oracleText = "Flying\nHeroic — Whenever you cast a spell that targets this creature, scry 1. " +
        "(To scry 1, look at the top card of your library, then you may put that card on the bottom.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.Scry(1)
        description = "Heroic — Whenever you cast a spell that targets this creature, scry 1."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "33"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e576f7a-0390-4514-aaed-20406fe6acdf.jpg?1783939572"
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
