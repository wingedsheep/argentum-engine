package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Edgewall Innkeeper — Throne of Eldraine #151
 * {G} · Creature — Human Peasant · 1/1
 *
 * Whenever you cast a creature spell that has an Adventure, draw a card.
 *
 * A cast trigger whose spell filter is `Creature ∧ HasAdventure`. [CardPredicate.HasAdventure] is a
 * whole-card layout characteristic, so it matches the creature half whether cast from hand or from
 * exile after its Adventure. An adventurer card cast *as* its Adventure is an instant or sorcery on
 * the stack with only the Adventure's characteristics (CR 715.3b), so the `Creature` half of the
 * filter keeps it from triggering — matching the ruling that such a spell isn't found.
 */
val EdgewallInnkeeper = card("Edgewall Innkeeper") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Peasant"
    power = 1
    toughness = 1
    oracleText = "Whenever you cast a creature spell that has an Adventure, draw a card. " +
        "(It doesn't need to have gone on the adventure first.)"

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature.withCardPredicate(CardPredicate.HasAdventure))
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "151"
        artist = "Matt Stewart"
        flavorText = "\"Warmth. Safety. A fully-stocked larder. You'll find nothing like Edgewall Keep in the " +
            "wilds, I promise you that!\""
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7c5d0560-f9e6-4c70-8cce-cae61e4e74bc.jpg?1783932613"
        ruling(
            "2019-10-04",
            "Edgewall Innkeeper's ability resolves before the spell that caused it to trigger. It resolves " +
                "even if that spell is countered."
        )
        ruling(
            "2019-10-04",
            "An effect may refer to a card, spell, or permanent that \"has an Adventure.\" This refers to " +
                "a card, spell, or permanent that has an adventurer card's set of alternative " +
                "characteristics, even if they're not being used and even if that card was never cast as an Adventure."
        )
        ruling(
            "2019-10-04",
            "If an effect refers to a card, spell, or permanent that has an Adventure, it won't find an " +
                "instant or sorcery spell on the stack that's been cast as an Adventure."
        )
    }
}
