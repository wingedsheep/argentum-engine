package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Markov Warlord
 * {5}{R}
 * Creature — Vampire Warrior
 * 4/4
 *
 * Haste
 * When this creature enters, up to two target creatures can't block this turn.
 *
 * "Up to two target creatures" is one requirement with `count = 2, optional = true`, and the
 * restriction is applied per chosen target through [Effects.ForEachTarget] over
 * [EffectTarget.ContextTarget]`(0)` — the same shape as Quakefoot Cyclops.
 */
val MarkovWarlord = card("Markov Warlord") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire Warrior"
    power = 4
    toughness = 4
    oracleText = "Haste (This creature can attack and {T} as soon as it comes under your control.)\n" +
        "When this creature enters, up to two target creatures can't block this turn."

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(TargetFilter.Creature, count = 2, optional = true)
        effect = Effects.ForEachTarget(Effects.CantBlock(EffectTarget.ContextTarget(0)))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Cynthia Sheppard"
        flavorText = "\"What use is a stake or holy symbol in the hands of a coward?\""
        imageUri = "https://cards.scryfall.io/normal/front/5/0/5035276f-31b9-4dd3-9ec8-42a664bdbd5c.jpg"
    }
}
