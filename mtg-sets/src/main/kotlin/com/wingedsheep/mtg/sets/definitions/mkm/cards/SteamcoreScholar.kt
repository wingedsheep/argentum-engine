package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Steamcore Scholar — Murders at Karlov Manor #71
 * {2}{U} · Creature — Weird Detective · 2/2
 *
 * Flying, vigilance
 * When this creature enters, draw two cards. Then discard two cards unless you discard an instant
 * or sorcery card or a creature card with flying.
 *
 * The "unless" is a choice the controller makes as the trigger resolves, not a condition checked
 * beforehand: two cards of any kind, or a single card with one of the listed characteristics. That
 * is [Patterns.Hand.discardCardsUnlessMatching] — one choose-exactly-two selection carrying a
 * `ReducedMinimumIfMatches` restriction that drops the minimum to one the moment the selection
 * holds a qualifying card. Modelling it as a prior modal choice would be wrong: the printed ruling
 * lets you pitch two cards even when one of them qualifies.
 *
 * The two draws and the discard live in one trigger, so the freshly drawn cards are in hand and
 * legal to discard.
 *
 * The qualifying filter is a homogeneous OR — instant-or-sorcery, or a creature card carrying
 * flying — which collapses to a single flat `CardPredicate.Or`. Keywords on a card in hand come
 * from its printed `baseKeywords` (there is no battlefield projection to read), so "creature card
 * with flying" is the card's own printed flying, exactly as the text means it.
 */
val SteamcoreScholar = card("Steamcore Scholar") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Weird Detective"
    power = 2
    toughness = 2
    oracleText = "Flying, vigilance\n" +
        "When this creature enters, draw two cards. Then discard two cards unless you discard an " +
        "instant or sorcery card or a creature card with flying."

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.Composite(
            Effects.DrawCards(2),
            Patterns.Hand.discardCardsUnlessMatching(
                count = 2,
                unlessFilter = GameObjectFilter.InstantOrSorcery or
                    GameObjectFilter.Creature.withKeyword(Keyword.FLYING),
                prompt = "Discard two cards, or a single instant, sorcery, or creature card with flying",
            ),
        )
        description = "When this creature enters, draw two cards. Then discard two cards unless " +
            "you discard an instant or sorcery card or a creature card with flying."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "71"
        artist = "David Astruga"
        flavorText = "Hm. That's weird."
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6bbf7394-9b17-45f8-a25b-d865e8452b2c.jpg?1783912907"

        ruling(
            "2024-02-02",
            "You can discard an instant card, a sorcery card, a creature card with flying, or any " +
                "two cards, even if one or both of those cards have the listed characteristics."
        )
    }
}
