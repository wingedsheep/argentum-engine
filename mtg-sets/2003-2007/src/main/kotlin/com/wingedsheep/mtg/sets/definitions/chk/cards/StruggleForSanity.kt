package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.RepeatCondition

/**
 * Struggle for Sanity — Champions of Kamigawa #145 (canonical printing)
 * {2}{B}{B} · Sorcery
 *
 * Target opponent reveals their hand. That player exiles a card from it, then you exile a card
 * from it. Repeat this process until all cards in that hand have been exiled. That player returns
 * the cards they exiled this way to their hand and puts the rest into their graveyard.
 *
 * Each pass of the loop re-gathers the hand and makes the two picks; `repeatCollecting` unions
 * both players' picks across every pass, so after the hand is empty the opponent's picks go back
 * to hand and yours go to the graveyard. When the opponent exiles the hand's last card, your pick
 * that pass is empty.
 */
val StruggleForSanity = card("Struggle for Sanity") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. That player exiles a card from it, then you exile " +
        "a card from it. Repeat this process until all cards in that hand have been exiled. That player " +
        "returns the cards they exiled this way to their hand and puts the rest into their graveyard."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val (exiledByThem, exiledByYou) = repeatCollecting(
                RepeatCondition.WhileCondition(Exists(opponent.asPlayer, Zone.HAND))
            ) {
                val hand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
                val (theirPick, rest) = chooseExactlySplit(
                    1,
                    from = hand,
                    chooser = Chooser.TargetPlayer,
                    prompt = "Choose a card from your hand to exile (you'll get it back)"
                )
                exile(theirPick, opponent.asPlayer)
                val yourPick = chooseExactly(
                    1,
                    from = rest,
                    chooser = Chooser.Controller,
                    prompt = "Choose a card from their hand to exile (it goes to their graveyard)",
                    showAllCards = true
                )
                exile(yourPick, opponent.asPlayer)
                listOf(theirPick, yourPick)
            }
            toHand(exiledByThem, opponent.asPlayer)
            toGraveyard(exiledByYou, opponent.asPlayer)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "145"
        artist = "Randy Gallegos"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba827c43-5dd5-471f-83b2-cb5428fcd063.jpg?1783944307"
    }
}
