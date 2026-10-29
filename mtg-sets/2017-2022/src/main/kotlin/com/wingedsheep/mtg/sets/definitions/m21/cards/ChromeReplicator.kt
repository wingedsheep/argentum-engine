package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Chrome Replicator
 * {5}
 * Artifact Creature — Construct
 * 4/4
 *
 * When this creature enters, if you control two or more nonland, nontoken permanents with the
 * same name as one another, create a 4/4 colorless Construct artifact creature token.
 *
 * The intervening "if" reads the largest same-name group among your nonland, nontoken
 * permanents (projected names), checked both as the trigger fires and as it resolves; the pair
 * need not be the same both times, and one token is made no matter how many pairs (rulings).
 */
val ChromeReplicator = card("Chrome Replicator") {
    manaCost = "{5}"
    typeLine = "Artifact Creature — Construct"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, if you control two or more nonland, nontoken permanents with " +
        "the same name as one another, create a 4/4 colorless Construct artifact creature token."

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.NonlandPermanent.nontoken())
                .largestSameNameGroup(),
            ComparisonOperator.GTE,
            2
        )
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            creatureTypes = setOf("Construct"),
            artifactToken = true,
            imageUri = "https://cards.scryfall.io/normal/front/4/0/4001c4c6-647c-4ffc-9803-81df43b77e12.jpg?1783930588"
        )
        description = "When this creature enters, if you control two or more nonland, nontoken permanents with " +
            "the same name as one another, create a 4/4 colorless Construct artifact creature token."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "229"
        artist = "Daniel Ljunggren"
        flavorText = "Who would you trust to watch your back more than yourself?"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e01bcb4-e823-4da5-b433-e75be3356367.jpg?1783930658"
        ruling("2020-06-23", "If you don't control two nonland, nontoken permanents with the same name immediately after Chrome Replicator enters the battlefield, its ability doesn't trigger. If you don't control two as the ability resolves, you don't create a token. They don't have to be the same pair, however.")
        ruling("2020-06-23", "Chrome Replicator's ability creates just one Construct, no matter how many namesake pairs of permanents you control.")
    }
}
