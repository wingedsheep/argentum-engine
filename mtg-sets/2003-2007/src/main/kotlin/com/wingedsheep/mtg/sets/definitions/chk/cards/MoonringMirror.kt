package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Moonring Mirror — Champions of Kamigawa #262
 * {5} · Artifact · Rare
 *
 * Whenever you draw a card, exile the top card of your library face down.
 * At the beginning of your upkeep, you may exile all cards from your hand face down. If you do,
 * put all other cards you own exiled with this artifact into your hand.
 *
 * Modelling notes:
 * - Both abilities exile into the artifact's linked-exile pile, face down and hidden (nobody may
 *   look at them). The draw trigger is a trigger, not a replacement (2004-12-01 ruling).
 * - "All **other** cards": the pile is gathered (and narrowed to cards you own) *before* the hand
 *   is exiled, so the cards just exiled from your hand stay exiled — the second 2004-12-01 ruling.
 * - Choosing "yes" is the "if you do"; an empty hand exiles nothing but still returns the pile.
 */
val MoonringMirror = card("Moonring Mirror") {
    manaCost = "{5}"
    typeLine = "Artifact"
    oracleText = "Whenever you draw a card, exile the top card of your library face down.\n" +
        "At the beginning of your upkeep, you may exile all cards from your hand face down. If you " +
        "do, put all other cards you own exiled with this artifact into your hand."

    triggeredAbility {
        trigger = Triggers.you.draws()
        effect = Effects.Pipeline {
            val top = gather(CardSource.TopOfLibrary(count = 1, player = Player.You))
            exile(top, faceDown = FaceDownMode.HIDDEN, linkToSource = true)
        }
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        effect = Effects.May(
            Effects.Pipeline {
                val pile = gather(CardSource.FromLinkedExile())
                val yours = filter(pile, GameObjectFilter.Any.ownedByYou())
                val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
                exile(hand, faceDown = FaceDownMode.HIDDEN, linkToSource = true)
                toHand(yours)
            },
            descriptionOverride = "You may exile all cards from your hand face down. If you do, put " +
                "all other cards you own exiled with this artifact into your hand."
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "262"
        artist = "Christopher Rush"
        imageUri = "https://cards.scryfall.io/normal/front/8/d/8d36357a-97f6-4b2f-abbf-43f84c29609d.jpg?1783944276"

        ruling("2004-12-01", "Note that Moonring Mirror's first ability is not a replacement effect.")
        ruling(
            "2004-12-01",
            "If you choose to use Moonring Mirror's second ability, you return all the cards you own " +
                "exiled by both of its abilities, but not any of the cards you just exiled from your hand."
        )
    }
}
