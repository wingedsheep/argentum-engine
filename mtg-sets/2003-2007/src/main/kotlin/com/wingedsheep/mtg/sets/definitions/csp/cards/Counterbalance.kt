package com.wingedsheep.mtg.sets.definitions.csp.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Counterbalance — Coldsnap #31
 * {U}{U} · Enchantment · Uncommon
 *
 * Whenever an opponent casts a spell, you may reveal the top card of your library. If you do,
 * counter that spell if it has the same mana value as the revealed card.
 *
 * The "may" is answered blind — you decide to reveal before seeing the card — so it is an outer
 * `May` around the whole reveal, not Delver's look-then-choose-to-reveal. The top card is gathered
 * without moving (it stays on top) and revealed to everyone. "If you do" fails when the library is
 * empty: nothing can be revealed, and the empty collection's mana value would otherwise read as 0
 * and counter a mana-value-0 spell, so the counter is gated on the revealed collection being
 * non-empty as well as on the mana values matching.
 */
val Counterbalance = card("Counterbalance") {
    manaCost = "{U}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "Whenever an opponent casts a spell, you may reveal the top card of your library. " +
        "If you do, counter that spell if it has the same mana value as the revealed card."

    triggeredAbility {
        trigger = Triggers.anOpponent.casts()
        effect = Effects.May(
            Effects.Pipeline {
                val counterbalanceRevealed = gather(CardSource.TopOfLibrary(1, Player.You))
                reveal(counterbalanceRevealed)
                run(
                    Effects.If(
                        condition = Conditions.All(
                            whenMatches(counterbalanceRevealed),
                            Conditions.CompareAmounts(
                                DynamicAmounts.manaValueOf(counterbalanceRevealed),
                                ComparisonOperator.EQ,
                                DynamicAmounts.triggeringManaValue(),
                            ),
                        ),
                        then = Effects.CounterTriggeringSpell(),
                    )
                )
            },
            prompt = "Reveal the top card of your library?",
        )
        description = "Whenever an opponent casts a spell, you may reveal the top card of your " +
            "library. If you do, counter that spell if it has the same mana value as the revealed card."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "31"
        artist = "John Zeleznik"
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c329ff2b-0331-4934-a8df-870dd7bf402b.jpg?1783943361"
        ruling(
            "2017-04-18",
            "If an opponent casts half of a split card (for example, Hit), it won't be countered if you " +
                "reveal that same split card. The mana value of a split card is determined by the combined " +
                "mana cost of its two halves, but the mana value of Hit is determined using only that half's " +
                "mana cost."
        )
        ruling(
            "2006-07-15",
            "If an opponent casts a spell with X in its mana cost, the mana value of that spell takes the " +
                "value of X into account. If you reveal a card with X in its mana cost, X is 0. For example, " +
                "if your opponent casts Blaze with X=1, Counterbalance will counter that spell if you reveal a " +
                "card with mana value 2, but it won't counter that spell if you reveal a Blaze of your own."
        )
    }
}
