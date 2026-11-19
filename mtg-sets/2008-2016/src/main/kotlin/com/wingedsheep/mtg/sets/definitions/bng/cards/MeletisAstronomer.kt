package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate

/**
 * Meletis Astronomer — Born of the Gods #43
 * {1}{U}
 * Creature — Human Wizard
 * 1/3
 * Heroic — Whenever you cast a spell that targets this creature, look at the top three cards of
 * your library. You may reveal an enchantment card from among them and put it into your hand. Put
 * the rest on the bottom of your library in any order.
 */
val MeletisAstronomer = card("Meletis Astronomer") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 3
    oracleText = "Heroic — Whenever you cast a spell that targets this creature, look at the top three cards " +
        "of your library. You may reveal an enchantment card from among them and put it into your hand. " +
        "Put the rest on the bottom of your library in any order."

    triggeredAbility {
        trigger = Triggers.you.casts(requires = setOf(SpellCastPredicate.TargetsSource))
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 3,
            filter = GameObjectFilter.Enchantment,
            prompt = "You may reveal an enchantment card from among them and put it into your hand",
            restOrder = CardOrder.ControllerChooses
        )
        description = "Heroic — Whenever you cast a spell that targets this creature, look at the top three " +
            "cards of your library. You may reveal an enchantment card from among them and put it into your " +
            "hand. Put the rest on the bottom of your library in any order."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "43"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af9adca9-3e69-474f-896d-bb05b1d67252.jpg?1783939569"
        ruling("2013-09-15", "Heroic abilities will resolve before the spell that caused them to trigger.")
        ruling("2013-09-15", "Heroic abilities will trigger only once per spell, even if that spell targets the creature with the heroic ability multiple times.")
        ruling("2013-09-15", "Heroic abilities won't trigger when a copy of a spell is created on the stack or when a spell's targets are changed to include a creature with a heroic ability.")
    }
}
