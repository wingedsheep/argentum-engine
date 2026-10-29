package com.wingedsheep.mtg.sets.definitions.tmp.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Scroll Rack — Tempest #308 (canonical printing)
 * {2} · Artifact
 *
 * {1}, {T}: Exile any number of cards from your hand face down. Put that many cards from the top
 * of your library into your hand. Then look at the exiled cards and put them on top of your
 * library in any order.
 *
 * A gather → choose → move pipeline in the printed order: the chosen hand cards are exiled face
 * down ([FaceDownMode.HIDDEN], still lookable by you), "that many" is the count of cards that
 * actually moved, the top cards go to hand by a plain move (not a draw — ruling: a short library
 * just yields fewer cards and you don't lose), and the exiled cards go back on top in the order
 * you choose.
 */
val ScrollRack = card("Scroll Rack") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "{1}, {T}: Exile any number of cards from your hand face down. Put that many " +
        "cards from the top of your library into your hand. Then look at the exiled cards and " +
        "put them on top of your library in any order."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
            val chosen = chooseAnyNumber(
                from = hand,
                prompt = "Exile any number of cards from your hand face down",
                selectedLabel = "Exile face down",
            )
            val exiled = moveTracked(
                chosen,
                CardDestination.ToZone(Zone.EXILE, Player.You),
                faceDown = FaceDownMode.HIDDEN,
                lookableInExile = true,
            )
            val topCards = gather(CardSource.TopOfLibrary(exiled.count))
            toHand(topCards)
            toLibraryTop(exiled)
        }
        description = "{1}, {T}: Exile any number of cards from your hand face down. Put that " +
            "many cards from the top of your library into your hand. Then look at the exiled " +
            "cards and put them on top of your library in any order."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "308"
        artist = "Heather Hudson"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5f7346d8-1aef-4618-88e6-74bd8865e0f3.jpg?1783946601"
        ruling("2004-10-04", "If there are not enough cards in the library, you do not lose. This is not a draw effect. You just get as many cards as are there, even if there are zero cards in the library.")
    }
}
