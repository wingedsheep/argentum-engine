package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser

/**
 * Traumatic Revelation — March of the Machine #127
 * {1}{B} · Sorcery
 *
 * Target opponent reveals their hand. You may choose a creature or battle card from it. If you
 * do, that player discards that card. If you don't, incubate 3.
 *
 * The "you may" is a choose-up-to-one over the revealed hand; the "if you don't" branch fires
 * when nothing was discarded — whether you declined or the hand held no creature or battle card.
 */
val TraumaticRevelation = card("Traumatic Revelation") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent reveals their hand. You may choose a creature or battle card from it. " +
        "If you do, that player discards that card. If you don't, incubate 3. (Create an Incubator " +
        "token with three +1/+1 counters on it and \"{2}: Transform this token.\" It transforms into " +
        "a 0/0 Phyrexian artifact creature.)"

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            run(Effects.RevealHand(opponent))
            val hand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer))
            run(
                Effects.IfYouDo(
                    action = Effects.Pipeline {
                        val chosen = chooseUpTo(
                            1,
                            from = hand,
                            chooser = Chooser.Controller,
                            filter = GameObjectFilter.Creature or GameObjectFilter.Battle,
                            prompt = "You may choose a creature or battle card to discard",
                            alwaysPrompt = true,
                            showAllCards = true
                        )
                        discard(chosen, opponent.asPlayer)
                    },
                    then = Effects.Nothing,
                    otherwise = Effects.Incubate(3)
                )
            )
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "127"
        artist = "Cristi Balanescu"
        flavorText = "\"My new form must be hard to understand.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59d40511-c9ff-466c-8eb9-cc7afc5c2eab.jpg?1783917000"
    }
}
