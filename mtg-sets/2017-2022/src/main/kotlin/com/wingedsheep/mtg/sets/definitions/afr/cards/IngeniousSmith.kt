package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ingenious Smith
 * {1}{W}
 * Creature — Human Artificer
 * 1/1
 * When this creature enters, look at the top four cards of your library. You may reveal an artifact
 * card from among them and put it into your hand. Put the rest on the bottom of your library in a
 * random order.
 * Whenever one or more artifacts you control enter, put a +1/+1 counter on this creature. This
 * ability triggers only once each turn.
 */
val IngeniousSmith = card("Ingenious Smith") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Artificer"
    oracleText = "When this creature enters, look at the top four cards of your library. You may reveal an artifact card from among them and put it into your hand. Put the rest on the bottom of your library in a random order.\nWhenever one or more artifacts you control enter, put a +1/+1 counter on this creature. This ability triggers only once each turn."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.lookAtTopRevealMatchingToHand(
            count = 4,
            filter = GameObjectFilter.Artifact,
            prompt = "You may reveal an artifact card from among them and put it into your hand"
        )
    }

    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Artifact).enter()
        oncePerTurn = true
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Nicholas Elias"
        imageUri = "https://cards.scryfall.io/normal/front/2/a/2ad29773-7c1b-410d-9f69-7aa751f6ebca.jpg?1783926531"
    }
}
