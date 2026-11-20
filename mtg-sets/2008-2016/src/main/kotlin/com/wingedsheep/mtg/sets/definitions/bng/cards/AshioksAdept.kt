package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Ashiok's Adept — Born of the Gods #59 (canonical printing)
 * {2}{B}
 * Creature — Human Wizard
 * 1/3
 * Heroic — Whenever you cast a spell that targets this creature, each opponent discards a card.
 */
val AshioksAdept = card("Ashiok's Adept") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 3
    oracleText = "Heroic — Whenever you cast a spell that targets this creature, each opponent discards a card."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Effects.EachOpponentDiscards(1)
        description = "Heroic — Whenever you cast a spell that targets this creature, each opponent discards a card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "59"
        artist = "Karla Ortiz"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86acd1cc-5238-4734-a324-b4cecffc0bab.jpg?1783939561"
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
