package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wishful Merfolk
 * {1}{U}
 * Creature — Merfolk
 * 3/2
 * Defender
 * {1}{U}: This creature loses defender and becomes a Human until end of turn.
 *
 * "Becomes a Human" overwrites its creature types (no "in addition"), so while it's a Human it's
 * no longer a Merfolk — `SetCreatureSubtypes` in layer 4, per the ruling.
 */
val WishfulMerfolk = card("Wishful Merfolk") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk"
    power = 3
    toughness = 2
    oracleText = "Defender\n{1}{U}: This creature loses defender and becomes a Human until end of turn."

    keywords(Keyword.DEFENDER)

    activatedAbility {
        cost = Costs.Mana("{1}{U}")
        effect = Effects.RemoveKeyword(Keyword.DEFENDER, EffectTarget.Self, Duration.EndOfTurn) then
            Effects.SetCreatureSubtypes(setOf(Subtype.HUMAN.value), EffectTarget.Self, Duration.EndOfTurn)
        description = "This creature loses defender and becomes a Human until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "73"
        artist = "Livia Prima"
        flavorText = "She yearned to walk on dry land so she might take her vengeance there."
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f9358d5d-726e-43e0-a58e-4cfe7c755913.jpg?1783932647"
        ruling("2019-10-04", "While Wishful Merfolk is a Human, it's no longer a Merfolk.")
    }
}
