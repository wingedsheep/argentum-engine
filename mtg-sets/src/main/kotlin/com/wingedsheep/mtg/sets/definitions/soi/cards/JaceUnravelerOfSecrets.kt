package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Jace, Unraveler of Secrets - {3}{U}{U}
 * Legendary Planeswalker — Jace
 * Starting Loyalty: 5
 *
 * +1: Scry 1, then draw a card.
 * −2: Return target creature to its owner's hand.
 * −8: You get an emblem with "Whenever an opponent casts their first spell each turn, counter
 *     that spell."
 *
 * The emblem is a permanent [Effects.CreateGlobalTriggeredAbility] whose trigger is
 * [Triggers.NthSpellCast] with n = 1 scoped to [Player.EachOpponent] — the engine already tracks
 * a per-turn, per-player spell count, so "their first spell each turn" is the n = 1 rung of the
 * same mechanism Shackle Slinger uses for "your second spell each turn". Scoping to
 * `EachOpponent` (rather than a single opponent) is what makes the emblem fire once per turn for
 * *each* opponent in a multiplayer game, and counting per caster is why a spell that can't be
 * countered still consumes that opponent's trigger for the turn.
 */
val JaceUnravelerOfSecrets = card("Jace, Unraveler of Secrets") {
    manaCost = "{3}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Planeswalker — Jace"
    startingLoyalty = 5
    oracleText = "+1: Scry 1, then draw a card.\n" +
        "−2: Return target creature to its owner's hand.\n" +
        "−8: You get an emblem with \"Whenever an opponent casts their first spell each turn, " +
        "counter that spell.\""

    loyaltyAbility(+1) {
        effect = Patterns.Library.scry(1).then(Effects.DrawCards(1))
    }

    loyaltyAbility(-2) {
        val creature = target("creature", Targets.Creature)
        effect = Effects.ReturnToHand(creature)
    }

    loyaltyAbility(-8) {
        effect = Effects.CreateGlobalTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.NthSpellCast(1, Player.EachOpponent).event,
                binding = Triggers.NthSpellCast(1, Player.EachOpponent).binding,
                effect = Effects.CounterTriggeringSpell()
            ),
            descriptionOverride = "Whenever an opponent casts their first spell each turn, counter that spell."
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "69"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/2/0/20d5521d-e9f1-49e0-aa13-8e6de794cb12.jpg?1783937795"

        ruling("2016-04-08", "The emblem's triggered ability counters the first spell an opponent casts on each turn, not just that opponent's turn.")
        ruling("2016-04-08", "If Jace's emblem's triggered ability doesn't counter the first spell an opponent casts (perhaps because that spell can't be countered), it won't trigger again in the same turn to try to counter that player's second spell.")
        ruling("2016-04-08", "If you have multiple opponents, Jace's emblem can trigger once each turn for each opponent.")
    }
}
