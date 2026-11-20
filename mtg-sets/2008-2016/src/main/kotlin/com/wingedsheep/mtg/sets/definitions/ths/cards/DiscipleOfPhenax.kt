package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Disciple of Phenax
 * {2}{B}{B}
 * Creature — Human Cleric
 * 1/3
 * When this creature enters, target player reveals a number of cards from their hand equal to your
 * devotion to black. You choose one of them. That player discards that card.
 *
 * Same two-chooser pipeline as Thieving Sprite: the targeted player picks which cards to reveal
 * (clamped to their hand size), then the Disciple's controller picks one of those to be discarded.
 * Devotion is counted as the trigger resolves, including the Disciple if it's still on the
 * battlefield; at devotion 0 nothing is revealed and nothing is discarded.
 */
val DiscipleOfPhenax = card("Disciple of Phenax") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 3
    oracleText = "When this creature enters, target player reveals a number of cards from their hand " +
        "equal to your devotion to black. You choose one of them. That player discards that card. " +
        "(Each {B} in the mana costs of permanents you control counts toward your devotion to black.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        target(Targets.Player)
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.TargetPlayer))
            val revealed = chooseExactly(
                DynamicAmounts.devotionTo(Color.BLACK),
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
            move(
                chosen,
                CardDestination.ToZone(Zone.GRAVEYARD, Player.TargetPlayer),
                moveType = MoveType.Discard
            )
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "84"
        artist = "John Severin Brassell"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81c1137c-1cbd-4918-9d1c-a9d2bd88562e.jpg?1783939781"
        ruling(
            "2013-09-15",
            "Use your devotion to black as the triggered ability resolves to determine how many cards will " +
                "be revealed. If Disciple of Phenax is still on the battlefield at that time, its mana cost " +
                "will count toward your devotion to black."
        )
        ruling(
            "2013-09-15",
            "Hybrid mana symbols, monocolored hybrid mana symbols, and Phyrexian mana symbols do count " +
                "toward your devotion to their color(s)."
        )
    }
}
