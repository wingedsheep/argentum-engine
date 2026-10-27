package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Emeria Captain — Zendikar Rising #11
 * {3}{W} · Creature — Angel Warrior · 1/1
 *
 * Flying, vigilance
 * When this creature enters, put a +1/+1 counter on it for each creature in your party.
 */
val EmeriaCaptain = card("Emeria Captain") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel Warrior"
    oracleText = "Flying, vigilance\nWhen this creature enters, put a +1/+1 counter on it for each creature " +
        "in your party. (Your party consists of up to one each of Cleric, Rogue, Warrior, and Wizard.)"
    power = 1
    toughness = 1

    keywords(Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.AddDynamicCounters(CounterType.PLUS_ONE_PLUS_ONE, DynamicAmounts.partySize(), EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "11"
        artist = "Matt Stewart"
        flavorText = "\"The gifts of the past must find their way into deserving hands.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/4/6490370e-fff5-4f17-b715-74ff8df5bc7c.jpg?1783929419"
    }
}
