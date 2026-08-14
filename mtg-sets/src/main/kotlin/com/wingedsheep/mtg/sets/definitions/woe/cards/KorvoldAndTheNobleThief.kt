package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Korvold and the Noble Thief
 * {3}{R}
 * Enchantment — Saga
 *
 * I, II — Create a Treasure token.
 * III — Exile the top three cards of target opponent's library. You may play those cards this turn.
 *
 * The third chapter targets the opponent, not their cards. The cards are gathered from that
 * player's library only when the chapter resolves, then moved to exile and granted to the Saga's
 * controller until end of turn. "Play" intentionally includes playing a land, subject to the
 * normal timing and one-land-per-turn rules.
 */
val KorvoldAndTheNobleThief = card("Korvold and the Noble Thief") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice " +
        "after III.)\n" +
        "I, II — Create a Treasure token. (It's an artifact with \"{T}, Sacrifice this token: " +
        "Add one mana of any color.\")\n" +
        "III — Exile the top three cards of target opponent's library. You may play those cards " +
        "this turn."

    sagaChapter(1) {
        effect = Effects.CreateTreasure()
    }

    sagaChapter(2) {
        effect = Effects.CreateTreasure()
    }

    sagaChapter(3) {
        target("target opponent", Targets.Opponent)
        effect = Effects.Composite(
            GatherCardsEffect(
                source = CardSource.TopOfLibrary(
                    count = DynamicAmount.Fixed(3),
                    player = Player.TargetOpponent,
                ),
                storeAs = "korvoldExiled",
            ),
            MoveCollectionEffect(
                from = "korvoldExiled",
                destination = CardDestination.ToZone(Zone.EXILE, player = Player.TargetOpponent),
            ),
            GrantMayPlayFromExileEffect(
                from = "korvoldExiled",
                expiry = MayPlayExpiry.EndOfTurn,
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "139"
        artist = "Ben Hill"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a811b2cb-ffc7-4100-ac3e-bc4125842bb2.jpg?1783915092"

        ruling(
            "2023-09-01",
            "You pay all costs and follow all normal timing rules for a card played this way. For " +
                "example, if the exiled card is a land card, you may play it only during your main " +
                "phase while the stack is empty.",
        )
    }
}
