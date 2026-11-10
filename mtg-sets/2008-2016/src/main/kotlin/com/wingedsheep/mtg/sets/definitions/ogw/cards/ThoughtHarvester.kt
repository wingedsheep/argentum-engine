package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Thought Harvester — Oath of the Gatewatch #48
 * {3}{U} · Creature — Eldrazi Drone · Uncommon
 * 2/4
 *
 * Devoid (This card has no color.)
 * Flying
 * Whenever you cast a colorless spell, target opponent exiles the top card of their library.
 *
 * Implementation notes:
 * - The cast trigger filters the spell on `CardPredicate.IsColorless` (Glaring Fleshraker's shape),
 *   so a devoid spell or an artifact counts, and the trigger resolves before that spell (ruling).
 * - The card goes to its owner's exile face up; an empty library exiles nothing.
 */
val ThoughtHarvester = card("Thought Harvester") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi Drone"
    power = 2
    toughness = 4
    oracleText = "Devoid (This card has no color.)\n" +
        "Flying\n" +
        "Whenever you cast a colorless spell, target opponent exiles the top card of their library."

    keywords(Keyword.DEVOID, Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Any.withCardPredicate(CardPredicate.IsColorless))
        val opponent = target(Targets.Opponent)
        effect = Patterns.Library.exileTop(1, opponent)
        description = "Whenever you cast a colorless spell, target opponent exiles the top card of their library."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "48"
        artist = "Winona Nelson"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/46f9eeb2-22f6-4bdb-bbf8-f53774cc6226.jpg?1783937920"
        ruling("2016-01-22", "The card is exiled face up.")
        ruling("2016-01-22", "The triggered ability will resolve before the colorless spell that caused it to trigger.")
        ruling("2016-01-22", "The Battle for Zendikar colorless creature cards Blight Herder and Ruin Processor each have an ability that triggers when you cast it that allows you to put a card owned by an opponent from exile into that player’s graveyard. Both that ability and Thought Harvester’s ability trigger at the same time, so you can put them on the stack in any order. If Thought Harvester’s ability resolves first (meaning it went on the stack last), the card exiled by that ability can be “processed” by the other ability.")
    }
}
