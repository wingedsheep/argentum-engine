package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference

/**
 * Spry and Mighty
 * {4}{G}
 * Sorcery
 * Choose exactly two creatures you control. You draw X cards and the chosen creatures
 * get +X/+X and gain trample until end of turn, where X is the difference between the
 * chosen creatures' powers.
 */
val SpryAndMighty = card("Spry and Mighty") {
    manaCost = "{4}{G}"
    typeLine = "Sorcery"
    oracleText = "Choose exactly two creatures you control. You draw X cards and the chosen creatures " +
        "get +X/+X and gain trample until end of turn, where X is the difference between the chosen " +
        "creatures' powers."

    spell {
        val (c1, c2) = targets(
            "creature",
            TargetCreature(count = 2, filter = TargetFilter.CreatureYouControl)
        )

        // X = |power(c1) - power(c2)| = max(p1 - p2, p2 - p1). Frozen into storedNumbers["x"]
        // so the first ModifyStats doesn't skew the second's reading of projected power.
        val p1 = DynamicAmount.EntityProperty(EntityReference.Target(0), EntityNumericProperty.Power)
        val p2 = DynamicAmount.EntityProperty(EntityReference.Target(1), EntityNumericProperty.Power)
        val diff = DynamicAmount.Max(
            DynamicAmount.Subtract(p1, p2),
            DynamicAmount.Subtract(p2, p1)
        )
        val x = DynamicAmount.VariableReference("x")

        effect = CompositeEffect(
            effects = listOf(
                Effects.StoreNumber("x", diff),
                Effects.DrawCards(x),
                Effects.ModifyStats(x, x, c1),
                Effects.ModifyStats(x, x, c2),
                Effects.GrantKeyword(Keyword.TRAMPLE, c1),
                Effects.GrantKeyword(Keyword.TRAMPLE, c2)
            ),
            descriptionOverride = "You draw {0} cards and the chosen creatures get +{0}/+{0} " +
                "and gain trample until end of turn.",
            descriptionAmounts = listOf(diff)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "195"
        artist = "Pete Venters"
        flavorText = "No matter their size, strength, or demeanor, the brave stand side by side."
        imageUri = "https://cards.scryfall.io/normal/front/1/5/152b7374-e991-443f-b6ca-914415635c4a.jpg?1767952201"
        ruling("2025-11-17", "If you don't control two creatures as Spry and Mighty resolves, the spell won't do anything.")
        ruling("2025-11-17", "To find the difference between the power of the chosen creatures, subtract the smaller of those two numbers from the larger one. For example, the difference between the power of a 3/3 creature and a 5/3 creature is 2.")
    }
}
