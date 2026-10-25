package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Choose Your Weapon
 * {2}{G}
 * Instant
 *
 * Choose one —
 * • Two-Weapon Fighting — Double target creature's power and toughness until end of turn.
 * • Archery — This spell deals 5 damage to target creature with flying.
 *
 * "Double" is the standard +X/+Y modification where X and Y are the target's power and toughness
 * as the effect begins to apply (read at resolution via [DynamicAmounts.powerOf] /
 * [DynamicAmounts.toughnessOf]); a negative power doubles downward, matching the rulings. The
 * Archery mode narrows its target to creatures with flying, checked against projected state.
 */
val ChooseYourWeapon = card("Choose Your Weapon") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Two-Weapon Fighting — Double target creature's power and toughness until end of turn.\n" +
        "• Archery — This spell deals 5 damage to target creature with flying."

    spell {
        modal(chooseCount = 1) {
            mode("Two-Weapon Fighting — Double target creature's power and toughness until end of turn.") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(
                    power = DynamicAmounts.powerOf(creature),
                    toughness = DynamicAmounts.toughnessOf(creature),
                    target = creature
                )
            }
            mode("Archery — This spell deals 5 damage to target creature with flying.") {
                val flier = target(TargetFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING)))
                effect = Effects.DealDamage(5, flier)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "175"
        artist = "Olivier Bernard"
        flavorText = "\"Move it, Varis, or I'll pick one for you!\"\n—Ellywick Tumblestrum"
        imageUri = "https://cards.scryfall.io/normal/front/2/1/213726e4-e2f5-4e52-b05a-21c4bab9927c.jpg?1783926466"
        ruling(
            "2021-07-23",
            "If an effect instructs you to \"double\" a creature's power, that creature gets +X/+0, " +
                "where X is its power as that effect begins to apply. The same is true for toughness."
        )
        ruling(
            "2021-07-23",
            "If a creature's power is less than 0 when it's doubled, instead that creature gets -X/-0, " +
                "where X is how much less than 0 its power is. For example, if an effect has given a 2/2 " +
                "creature -4/-0 so that it's a -2/2 creature, doubling its power and toughness gives it " +
                "-2/+2, and it becomes a -4/4 creature."
        )
    }
}
