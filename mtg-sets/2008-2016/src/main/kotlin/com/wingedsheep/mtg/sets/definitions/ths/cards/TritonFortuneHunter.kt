package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Triton Fortune Hunter — Theros #69 (canonical printing)
 * {2}{U}
 * Creature — Merfolk Soldier
 * 2/2
 * Heroic — Whenever you cast a spell that targets this creature, draw a card.
 *
 * Heroic is the `TargetsSource` cast trigger (Fabled Hero).
 */
val TritonFortuneHunter = card("Triton Fortune Hunter") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Soldier"
    power = 2
    toughness = 2
    oracleText = "Heroic — Whenever you cast a spell that targets this creature, draw a card."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.DrawCards(1)
        description = "Heroic — Whenever you cast a spell that targets this creature, draw a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "69"
        artist = "Clint Cearley"
        flavorText = "\"Thassa has blessed me with power and insight. I am careful not to disappoint her.\""
        imageUri = "https://cards.scryfall.io/normal/front/1/1/1173ff96-998c-4fe7-9b28-602d990e0339.jpg?1783939787"
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
