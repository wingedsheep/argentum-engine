package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Stolen Goods — Avacyn Restored #78 (canonical printing)
 * {3}{U} · Sorcery
 *
 * Target opponent exiles cards from the top of their library until they exile a nonland card.
 * Until end of turn, you may cast that card without paying its mana cost.
 *
 * Nicol Bolas, God-Pharaoh's +2 as a sorcery: walk the target opponent's library with
 * `gatherUntilMatch(Nonland, Player.TargetOpponent)`, exile everything revealed, then grant a
 * free cast of the found card until end of turn. Lands stay exiled, and an uncast card stays
 * exiled (rulings). An empty-of-nonlands library exiles everything and grants nothing.
 */
val StolenGoods = card("Stolen Goods") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Target opponent exiles cards from the top of their library until they exile a nonland card. " +
        "Until end of turn, you may cast that card without paying its mana cost."

    spell {
        target(Targets.Opponent)
        effect = Effects.Pipeline {
            val (found, revealed) = gatherUntilMatch(GameObjectFilter.Nonland, player = Player.TargetOpponent)
            exile(revealed)
            run(Effects.GrantMayPlayFromExile(found, expiry = MayPlayExpiry.EndOfTurn))
            run(Effects.GrantPlayWithoutPayingCost(found, expiry = MayPlayExpiry.EndOfTurn))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "78"
        artist = "Anthony Francisco"
        imageUri = "https://cards.scryfall.io/normal/front/5/3/53203dfc-5ad5-4c17-9802-ca2f874d327a.jpg?1783940709"
        ruling("2012-05-01", "You cast the card from exile. Each land card exiled this way will remain exiled.")
        ruling(
            "2012-05-01",
            "If you can't cast the nonland card (perhaps because there are no legal targets available) or if you " +
                "choose not to cast it, it will remain exiled."
        )
        ruling("2012-05-01", "You must follow the normal timing restrictions of the exiled card.")
        ruling(
            "2012-05-01",
            "If you cast a card \"without paying its mana cost,\" you can't pay any alternative costs. You can pay " +
                "additional costs, such as kicker costs. If the card has mandatory additional costs, you must pay those."
        )
        ruling("2012-05-01", "If the card has {X} in its mana cost, you must choose 0 as its value.")
        ruling(
            "2012-05-01",
            "If there are no nonland cards left in that library, the opponent will exile all cards from their " +
                "library, and Stolen Goods will finish resolving."
        )
    }
}
