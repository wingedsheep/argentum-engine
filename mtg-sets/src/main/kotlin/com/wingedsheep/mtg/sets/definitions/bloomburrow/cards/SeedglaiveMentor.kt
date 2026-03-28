package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Seedglaive Mentor
 * {1}{R}{W}
 * Creature — Mouse Soldier
 * 3/2
 *
 * Vigilance, haste
 *
 * Valiant — Whenever this creature becomes the target of a spell or ability
 * you control for the first time each turn, put a +1/+1 counter on it.
 */
val SeedglaiveMentor = card("Seedglaive Mentor") {
    manaCost = "{1}{R}{W}"
    typeLine = "Creature — Mouse Soldier"
    power = 3
    toughness = 2
    oracleText = "Vigilance, haste\nValiant — Whenever this creature becomes the target of a spell or ability you control for the first time each turn, put a +1/+1 counter on it."

    keywords(Keyword.VIGILANCE, Keyword.HASTE)

    // Valiant: put a +1/+1 counter on it
    triggeredAbility {
        trigger = Triggers.Valiant
        effect = Effects.AddCounters("+1/+1", 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "231"
        artist = "Vincent Christiaens"
        flavorText = "\"Your riposte is good, yes. But how's your footwork?\""
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d21c3e41-0636-49a3-8c9c-384c5e5c9c3e.jpg?1721427185"
    }
}
