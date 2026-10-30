package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bramblefort Fink
 * {1}{G}
 * Creature — Ouphe
 * 2/2
 *
 * {8}: This creature has base power and toughness 10/10 until end of turn. Activate only if you
 * control an Oko planeswalker.
 */
val BramblefortFink = card("Bramblefort Fink") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Ouphe"
    oracleText = "{8}: This creature has base power and toughness 10/10 until end of turn. Activate only if you control an Oko planeswalker."
    power = 2
    toughness = 2

    activatedAbility {
        cost = Costs.Mana("{8}")
        effect = Effects.SetBasePowerAndToughness(10, 10, EffectTarget.Self, Duration.EndOfTurn)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.YouControl(GameObjectFilter.Planeswalker.withSubtype("Oko"))
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "311"
        artist = "Uriah Voth"
        flavorText = "No matter the cruelty of Oko's jokes, he's always guaranteed the sycophant's laughter."
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc83a142-9d0f-4a39-baeb-4e2f62009204.jpg?1783932553"
    }
}
