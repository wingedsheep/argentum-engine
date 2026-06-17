package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Joined Researchers // Secret Rendezvous — Secrets of Strixhaven #23
 * {1}{W} · Creature — Human Cleric Wizard · 2/2
 *
 * First strike
 * At the beginning of each end step, if an opponent has more cards in hand than you, this
 * creature becomes prepared. (While it's prepared, you may cast a copy of its spell. Doing so
 * unprepares it.)
 * //
 * Secret Rendezvous — {1}{W}{W}, Sorcery: You and target opponent each draw three cards.
 *
 * Prepare (Secrets of Strixhaven): unlike most prepare cards, this creature does NOT enter
 * prepared — it has no PREPARED keyword. Instead its end-step trigger makes it become prepared
 * (via [com.wingedsheep.sdk.dsl.Effects.MakePrepared]) while an opponent is hoarding cards.
 * Becoming prepared creates a copy of its prepare spell ("Secret Rendezvous") in exile that its
 * controller may cast for {1}{W}{W}; casting that copy unprepares the creature. The prepare spell
 * itself is declared via the `prepare(name) { }` DSL ([com.wingedsheep.sdk.model.CardLayout.PREPARE]).
 */
val JoinedResearchers = card("Joined Researchers") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric Wizard"
    power = 2
    toughness = 2
    oracleText = "First strike\n" +
        "At the beginning of each end step, if an opponent has more cards in hand than you, " +
        "this creature becomes prepared. (While it's prepared, you may cast a copy of its spell. " +
        "Doing so unprepares it.)"

    keywords(Keyword.FIRST_STRIKE)

    // End-step trigger: if an opponent has more cards in hand than you, become prepared.
    triggeredAbility {
        trigger = Triggers.EachEndStep
        triggerCondition = Conditions.OpponentHasMoreCardsInHand
        effect = Effects.MakePrepared()
        description = "At the beginning of each end step, if an opponent has more cards in hand " +
            "than you, this creature becomes prepared."
    }

    // Secret Rendezvous — the prepare spell. You and target opponent each draw three cards.
    prepare("Secret Rendezvous") {
        manaCost = "{1}{W}{W}"
        typeLine = "Sorcery"
        oracleText = "You and target opponent each draw three cards."
        spell {
            target = Targets.Opponent
            effect = Effects.Composite(
                Effects.DrawCards(3),
                Effects.DrawCards(3, EffectTarget.ContextTarget(0))
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "23"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1ebaafe0-3a9a-424c-8698-d26e7be45343.jpg?1778165075"
    }
}
