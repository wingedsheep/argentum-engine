package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Counterlash — Dark Ascension #33
 * {4}{U}{U} · Instant · Rare
 *
 * Counter target spell. You may cast a spell that shares a card type with it from your hand
 * without paying its mana cost.
 *
 * Modelling notes:
 * - The free spell is picked with `chooseSpell` (up to one — declining is the "may") over your
 *   hand, filtered to cards whose castable face shares a card type with the target spell, then
 *   cast during resolution with [Effects.CastFromCollectionWithoutPayingCost] (timing permissions
 *   based on card type are ignored, X is 0, no alternative costs — the 2011-01-22 rulings).
 * - The choice is made while the target is still a spell on the stack, then the counter happens,
 *   then the chosen card is cast. "It" is the spell as it last existed on the stack (CR 608.2h): a
 *   countered modal double-faced back face is its front face again in the graveyard, and a
 *   countered copy ceases to exist, so reading the types after the counter would get those wrong.
 *   Hand contents aren't touched by the counter, so the ordering is otherwise invisible.
 * - A spell that can't be countered still lets you cast the free spell; an illegal target on
 *   resolution fizzles Counterlash entirely, as printed.
 */
val Counterlash = card("Counterlash") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Counter target spell. You may cast a spell that shares a card type with it from " +
        "your hand without paying its mana cost."

    spell {
        val countered = target(TargetFilter.SpellOnStack)
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
            val chosen = chooseSpell(
                from = hand,
                filter = GameObjectFilter.Nonland.sharingCardTypeWith(countered),
                showAllCards = true,
                prompt = "You may cast a spell that shares a card type with the countered spell " +
                    "without paying its mana cost",
                selectedLabel = "Cast for free",
            )
            run(Effects.CounterSpell())
            run(Effects.CastFromCollectionWithoutPayingCost(chosen))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "33"
        artist = "Austin Hsu"
        flavorText = "\"Pathetic. Let me show you how it's done.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d3ec2c57-8e67-472d-8f2e-0492d311f130.jpg?1783940844"
        ruling("2011-01-22", "You cast the card in your hand as part of the resolution of Counterlash. Timing restrictions based on the card's type (such as creature or sorcery) are ignored. Other restrictions are not (such as \"Cast [this card] only during combat\").")
        ruling("2011-01-22", "If you cast a card \"without paying its mana cost,\" you can't pay any alternative costs. You can pay additional costs, such as kicker costs. If the card has mandatory additional costs, you must pay those.")
        ruling("2011-01-22", "If the card has {X} in its mana cost, you must choose 0 as its value.")
    }
}
