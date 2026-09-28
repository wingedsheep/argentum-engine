package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Onakke Javelineer
 * {4}{R}
 * Creature — Ogre Spirit
 * 5/4
 * Reach
 * {T}: This creature deals 2 damage to target player or battle.
 */
val OnakkeJavelineer = card("Onakke Javelineer") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Ogre Spirit"
    power = 5
    toughness = 4
    oracleText = "Reach\n{T}: This creature deals 2 damage to target player or battle."

    keywords(Keyword.REACH)

    activatedAbility {
        cost = Costs.Tap
        val victim = target(Targets.PlayerOrBattle)
        effect = Effects.DealDamage(2, victim)
        description = "{T}: This creature deals 2 damage to target player or battle."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "156"
        artist = "Joseph Weston"
        flavorText = "With the ways between worlds thrown open, the Chain Veil's foul magic seeped from a vault deep within Ravnica all the way to Shandalar's dormant masters."
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e5446057-7330-40b9-a5d9-bad4876337cd.jpg?1783916985"
    }
}
