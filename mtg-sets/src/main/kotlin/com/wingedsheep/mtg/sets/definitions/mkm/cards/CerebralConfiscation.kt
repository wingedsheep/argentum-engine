package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetOpponent

/**
 * Cerebral Confiscation — Murders at Karlov Manor #81
 * {2}{B} · Sorcery
 *
 * Choose one —
 * • Target opponent discards two cards.
 * • Target opponent reveals their hand. You choose a nonland card from it. That player discards
 *   that card.
 *
 * Each mode targets independently, so the mode is chosen as the spell is cast and only that
 * mode's target is picked (CR 601.2b). Mode 1 is the opponent's own choice of two cards; mode 2
 * is the Pilfer / Ego Drain idiom — reveal, gather the revealed hand, then *you* pick one
 * nonland card. Both move with [MoveType.Discard] so discard triggers (madness, "whenever you
 * discard") see it.
 */
val CerebralConfiscation = card("Cerebral Confiscation") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Target opponent discards two cards.\n" +
        "• Target opponent reveals their hand. You choose a nonland card from it. That player discards that card."

    spell {
        modal(chooseCount = 1) {
            mode("Target opponent discards two cards") {
                val opponent = target("target opponent", TargetOpponent())
                effect = Effects.Discard(2, opponent)
            }
            mode("Target opponent reveals their hand — you choose a nonland card to discard") {
                val opponent = target("target opponent", TargetOpponent())
                effect = Effects.Pipeline {
                    run(RevealHandEffect(opponent))
                    val hand = gather(CardSource.FromZone(Zone.HAND, Player.ContextPlayer(0)), name = "opponentHand")
                    val chosen = chooseExactly(
                        1, from = hand,
                        filter = GameObjectFilter.Nonland,
                        prompt = "Choose a nonland card to discard",
                        alwaysPrompt = true,
                        showAllCards = true,
                        name = "toDiscard"
                    )
                    move(
                        chosen,
                        CardDestination.ToZone(Zone.GRAVEYARD, Player.ContextPlayer(0)),
                        moveType = MoveType.Discard
                    )
                }
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "81"
        artist = "Lius Lasahido"
        flavorText = "\"Never rely on secondhand description. Always review the memory yourself.\"\n—Lazav"
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c3c3d15-b775-44a2-91ea-4abcc3cf2dba.jpg?1783912903"
    }
}
