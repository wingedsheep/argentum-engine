package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ravaging Horde
 * {3}{R}{R}
 * Creature — Human Soldier
 */
val RavagingHorde = card("Ravaging Horde") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, destroy target land."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val land = target("target", Targets.Land)
        effect = Effects.Destroy(land)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "118"
        artist = "Liu Jianjian"
        flavorText = "Upon his return to the capitol after taking soldiers to loot a nearby peaceful town, the prime minister, Dong Zhou, boasted of it as a victory over bandits."
        imageUri = "https://cards.scryfall.io/normal/front/6/2/6278d679-fc54-4527-ab16-90735574ab9b.jpg"
    }
}
