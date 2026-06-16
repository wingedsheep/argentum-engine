package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.events.SpellCastPredicate
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Geometer's Arthropod
 * {G}{U}
 * Creature — Fractal Crab
 * 1/4
 *
 * Whenever you cast a spell with {X} in its mana cost, look at the top X cards of your library. Put
 * one of them into your hand and the rest on the bottom of your library in a random order.
 *
 * The trigger keys off the *printed cost* having `{X}` (`SpellCastPredicate.HasXInCost`), not the
 * value chosen — a spell cast with X=0 still triggers (and then looks at zero cards, a harmless
 * no-op). The dig count is the actual value announced for that spell's {X}, read via
 * `DynamicAmounts.xValueOfTriggeringSpell()`. The dig itself is the standard
 * gather → select-one → move-rest pipeline: keep one in hand, put the rest on the bottom in a
 * random order.
 */
val GeometersArthropod = card("Geometer's Arthropod") {
    manaCost = "{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Fractal Crab"
    power = 1
    toughness = 4
    oracleText = "Whenever you cast a spell with {X} in its mana cost, look at the top X cards of " +
        "your library. Put one of them into your hand and the rest on the bottom of your library " +
        "in a random order."

    triggeredAbility {
        trigger = Triggers.youCastSpell(
            requires = setOf(SpellCastPredicate.HasXInCost),
        )
        effect = Patterns.Library.lookAtTopAndKeep(
            count = DynamicAmounts.xValueOfTriggeringSpell(),
            keepCount = DynamicAmount.Fixed(1),
            keepDestination = CardDestination.ToZone(Zone.HAND),
            restDestination = CardDestination.ToZone(Zone.LIBRARY, placement = ZonePlacement.Bottom),
            restOrder = CardOrder.Random,
            selectedLabel = "Put in hand",
            remainderLabel = "Put on bottom",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "191"
        artist = "Joe Slucher"
        flavorText = "\"Creatures that have survived blood-soaked millennia should not be " +
            "underestimated.\"\n—Nev, dean of substance"
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec0f3613-1edc-40e8-8f26-2e5ef13be55e.jpg?1775938325"
    }
}
