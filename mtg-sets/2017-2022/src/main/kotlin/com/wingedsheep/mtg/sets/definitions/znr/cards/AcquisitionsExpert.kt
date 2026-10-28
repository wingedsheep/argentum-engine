package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Acquisitions Expert — Zendikar Rising #89
 * {1}{B} · Creature — Human Rogue · 1/2
 *
 * When this creature enters, target opponent reveals a number of cards from their hand equal to
 * the number of creatures in your party. You choose one of those cards. That player discards that
 * card.
 *
 * The Thieving Sprite / Cabal Interrogator shape: the *targeted opponent* picks which cards to
 * reveal (clamped to their hand size), then the controller picks one of those to be discarded.
 * The party size is read on resolution, so the Expert itself (a Rogue) counts if it's still on the
 * battlefield. A party of zero reveals nothing and discards nothing.
 */
val AcquisitionsExpert = card("Acquisitions Expert") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Rogue"
    power = 1
    toughness = 2
    oracleText = "When this creature enters, target opponent reveals a number of cards from their hand " +
        "equal to the number of creatures in your party. You choose one of those cards. That player " +
        "discards that card. (Your party consists of up to one each of Cleric, Rogue, Warrior, and Wizard.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            val revealed = chooseExactly(
                DynamicAmounts.partySize(),
                from = hand,
                chooser = Chooser.TargetPlayer,
                prompt = "Choose cards to reveal",
                selectedLabel = "Reveal"
            )
            reveal(revealed)
            val chosen = chooseExactly(
                1,
                from = revealed,
                chooser = Chooser.Controller,
                prompt = "Choose a revealed card for that player to discard",
                selectedLabel = "Discard"
            )
            discard(chosen, opponent.asPlayer)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "89"
        artist = "Anna Steinbauer"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/281336c7-ca7c-4c91-bdfb-d253a965cb7c.jpg?1783929383"
        ruling(
            "2020-09-25",
            "If there are no creatures in your party as Acquisitions Expert's ability resolves, the target " +
                "opponent reveals no cards and you choose no card."
        )
        ruling(
            "2020-09-25",
            "If an ability of a creature counts the number of creatures in your party, that number is counted " +
                "as the ability resolves. If that creature is still on the battlefield when the ability resolves, " +
                "it'll be counted if appropriate."
        )
    }
}
