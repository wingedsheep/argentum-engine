package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AssignDamageEqualToToughness
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Huatli, the Sun's Heart
 * {2}{G/W}
 * Legendary Planeswalker — Huatli
 * Loyalty 7
 *
 * Each creature you control assigns combat damage equal to its toughness rather than its power.
 * −3: You gain life equal to the greatest toughness among creatures you control.
 *
 * The static is unconditional (unlike Ancient Lumberknot's "with toughness greater than its
 * power"), so `onlyWhenToughnessGreaterThanPower = false`. The −3 reads the greatest toughness
 * on resolution, per the 2019-05-03 ruling; with no creatures it gains 0.
 */
val HuatliTheSunsHeart = card("Huatli, the Sun's Heart") {
    manaCost = "{2}{G/W}"
    colorIdentity = "GW"
    typeLine = "Legendary Planeswalker — Huatli"
    startingLoyalty = 7
    oracleText = "Each creature you control assigns combat damage equal to its toughness rather than its power.\n" +
        "−3: You gain life equal to the greatest toughness among creatures you control."

    // Each creature you control assigns combat damage equal to its toughness rather than its power.
    staticAbility {
        ability = AssignDamageEqualToToughness(
            filter = GroupFilter.AllCreaturesYouControl,
            onlyWhenToughnessGreaterThanPower = false,
        )
    }

    // −3: You gain life equal to the greatest toughness among creatures you control.
    loyaltyAbility(-3) {
        effect = Effects.GainLife(
            DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).maxToughness()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "230"
        artist = "Lius Lasahido"
        imageUri = "https://cards.scryfall.io/normal/front/d/7/d709742c-82a2-457e-9991-85e98d78029d.jpg?1783933379"
        ruling(
            "2019-05-03",
            "Huatli's first ability doesn't actually change any creature's power. It changes only the amount " +
                "of combat damage it assigns. All other rules and effects that check power or toughness use the " +
                "real values.",
        )
        ruling(
            "2019-05-03",
            "The greatest toughness among creatures you control is determined only as Huatli's loyalty " +
                "ability begins to resolve.",
        )
    }
}
