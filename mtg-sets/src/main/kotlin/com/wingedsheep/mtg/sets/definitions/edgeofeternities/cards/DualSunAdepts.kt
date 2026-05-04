package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Dual-Sun Adepts
 * {2}{W}
 * Creature — Human Soldier
 * Double strike
 * {5}: Creatures you control get +1/+1 until end of turn.
 */
val DualSunAdepts = card("Dual-Sun Adepts") {
    manaCost = "{2}{W}"
    typeLine = "Creature — Human Soldier"
    power = 2
    toughness = 2
    oracleText = "Double strike\n{5}: Creatures you control get +1/+1 until end of turn."

    // Double strike keyword
    keywords(Keyword.DOUBLE_STRIKE)

    // Activated ability: {5}: Creatures you control get +1/+1 until end of turn
    activatedAbility {
        cost = Costs.Mana("{5}")
        effect = EffectPatterns.modifyStatsForAll(1, 1, GroupFilter.AllCreaturesYouControl)
        description = "{5}: Creatures you control get +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "12"
        artist = "Ioannis Fiore"
        flavorText = "\"You fight beside me as brilliantly as the dawn. I have nothing more to teach you.\"\n—Master Iridiss"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7e8c830-77ae-437f-8e28-ce61c5fde6b6.jpg?1752946599"
    }
}
