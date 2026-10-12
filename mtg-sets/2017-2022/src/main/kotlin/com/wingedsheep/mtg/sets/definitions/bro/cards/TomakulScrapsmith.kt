package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tomakul Scrapsmith — The Brothers' War #153
 * {2}{R} · Creature — Human Artificer · 2/1
 *
 * When this creature enters, mill three cards. You may put an artifact card from among the
 * cards milled this way into your hand. If you don't, put a +1/+1 counter on this creature.
 *
 * Same shape as Blanchwood Prowler: an empty choice (declined, or no artifact milled) puts
 * the +1/+1 counter on this creature instead.
 */
val TomakulScrapsmith = card("Tomakul Scrapsmith") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Artificer"
    power = 2
    toughness = 1
    oracleText = "When this creature enters, mill three cards. You may put an artifact card from among " +
        "the cards milled this way into your hand. If you don't, put a +1/+1 counter on this creature. " +
        "(To mill a card, put the top card of your library into your graveyard.)"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val milled = mill(3)
            val artifacts = filter(milled, GameObjectFilter.Artifact)
            val chosen = chooseUpTo(1, from = artifacts)
            ifNotEmpty(chosen) {
                toHand(chosen)
            } orElse {
                run(Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self))
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "153"
        artist = "Caio Monteiro"
        imageUri = "https://cards.scryfall.io/normal/front/d/4/d462d30c-9060-46d6-b5b3-cac476ee2ce1.jpg?1783920059"
    }
}
