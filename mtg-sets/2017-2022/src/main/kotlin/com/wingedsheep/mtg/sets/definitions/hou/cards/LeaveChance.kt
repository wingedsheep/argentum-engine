package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Leave // Chance — Hour of Devastation #153
 *
 * Leave {1}{W} — Instant: Return any number of target permanents you own to your hand.
 * Chance {3}{R} — Sorcery: Aftermath (Cast this spell only from your graveyard. Then exile it.)
 *   Discard any number of cards, then draw that many cards.
 */
val LeaveChance = card("Leave // Chance") {
    layout = CardLayout.SPLIT
    colorIdentity = "RW"

    face("Leave") {
        manaCost = "{1}{W}"
        typeLine = "Instant"
        oracleText = "Return any number of target permanents you own to your hand."

        spell {
            targets(TargetFilter(GameObjectFilter.Permanent.ownedByYou()), optional = true, unlimited = true)
            effect = Effects.ForEachTarget(Effects.ReturnToHand(EffectTarget.ContextTarget(0)))
        }
    }

    face("Chance") {
        manaCost = "{3}{R}"
        typeLine = "Sorcery"
        oracleText = "Aftermath (Cast this spell only from your graveyard. Then exile it.)\n" +
            "Discard any number of cards, then draw that many cards."
        keywords(Keyword.AFTERMATH)

        spell {
            effect = Effects.Pipeline {
                val discarded = runStoringCollection { Patterns.Hand.discardAnyNumber(storeAs = it) }
                run(Effects.DrawCards(discarded.count))
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "153"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f928e8e8-aa20-402c-85bd-59106e9b9cc7.jpg?1783936006"
        ruling("2017-07-14", "You are a permanent's owner if the card representing it began the game in your deck, or if it's a token that entered the battlefield under your control. Leave can target a permanent you own but don't control.")
        ruling("2017-04-18", "A spell with aftermath cast from a graveyard will always be exiled afterward, whether it resolves, it's countered, or it leaves the stack in some other way.")
    }
}
