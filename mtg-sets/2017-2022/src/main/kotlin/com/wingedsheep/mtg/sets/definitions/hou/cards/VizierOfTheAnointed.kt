package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Vizier of the Anointed — Hour of Devastation #55
 * {3}{U} · Creature — Human Cleric · 2/4
 *
 * When this creature enters, you may search your library for a creature card with eternalize or
 * embalm, put that card into your graveyard, then shuffle.
 * Whenever you activate an eternalize or embalm ability, draw a card.
 *
 * Both halves name the mechanics through their keyword: the search reads the card's printed
 * `Keyword.ETERNALIZE` / `Keyword.EMBALM`, and the draw trigger reads the activated ability's own
 * `ActivatedAbility.keyword` marker, so a granted embalm (Cursecloth Wrappings) counts too while a
 * creature's other abilities don't. Per the ruling the card is drawn after the costs are paid and
 * before the ability resolves — the trigger goes on the stack above it.
 */
val VizierOfTheAnointed = card("Vizier of the Anointed") {
    manaCost = "{3}{U}"
    typeLine = "Creature — Human Cleric"
    power = 2
    toughness = 4
    oracleText = "When this creature enters, you may search your library for a creature card with eternalize or " +
        "embalm, put that card into your graveyard, then shuffle.\n" +
        "Whenever you activate an eternalize or embalm ability, draw a card."

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Creature.withKeyword(Keyword.ETERNALIZE) or
                GameObjectFilter.Creature.withKeyword(Keyword.EMBALM),
            destination = SearchDestination.GRAVEYARD
        )
    }

    triggeredAbility {
        trigger = Triggers.you.activatesAbility(keywords = setOf(Keyword.ETERNALIZE, Keyword.EMBALM))
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "55"
        artist = "Jakub Kasper"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68ef426c-b396-4767-b360-435f67207022.jpg?1783936045"
        ruling("2017-07-14", "If you activate an eternalize or embalm ability, you’ll draw a card before that ability resolves, but after you’ve paid all of the costs for that ability. If an eternalize ability requires a discard to activate, you’ll need to have another card available to discard.")
    }
}
