package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Borrowed Knowledge
 * {2}{R}{W}
 * Sorcery
 * Choose one —
 * • Discard your hand, then draw cards equal to the number of cards in target opponent's hand.
 * • Discard your hand, then draw cards equal to the number of cards discarded this way.
 *
 * Both modes share the "discard your hand" prefix ([Patterns.Hand.discardHand], which gathers the
 * hand into the `discardedHand` collection then discards it). The two modes differ only in how
 * many cards are then drawn:
 *  - Mode 1 draws `Count(TargetOpponent, HAND)` — read at resolution after the discard (the
 *    discard never touches the opponent's hand, so ordering is harmless).
 *  - Mode 2 draws `VariableReference("discardedHand_count")` — the size of the gathered collection
 *    that `discardHand` discarded ("the number of cards discarded this way"). `GatherCardsEffect`
 *    auto-publishes a `<storeAs>_count` variable (same convention as the wheel patterns).
 */
val BorrowedKnowledge = card("Borrowed Knowledge") {
    manaCost = "{2}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Discard your hand, then draw cards equal to the number of cards in target opponent's " +
        "hand.\n" +
        "• Discard your hand, then draw cards equal to the number of cards discarded this way."

    spell {
        modal(chooseCount = 1) {
            mode("Discard your hand, then draw cards equal to the number of cards in target opponent's hand") {
                target = Targets.Opponent
                effect = Effects.Composite(
                    Patterns.Hand.discardHand(),
                    Effects.DrawCards(DynamicAmount.Count(Player.TargetOpponent, Zone.HAND)),
                )
            }
            mode("Discard your hand, then draw cards equal to the number of cards discarded this way") {
                effect = Effects.Composite(
                    Patterns.Hand.discardHand(),
                    Effects.DrawCards(DynamicAmount.VariableReference("discardedHand_count")),
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "178"
        artist = "Inkognit"
        flavorText = "\"Professor, we study history and my neighbor wrote his answer first. I " +
            "was learning from the past.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a3226e14-554d-47c9-b8b6-dfeb53cc41ba.jpg?1775938224"
    }
}
