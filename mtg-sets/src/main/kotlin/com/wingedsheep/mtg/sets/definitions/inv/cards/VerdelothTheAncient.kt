package com.wingedsheep.mtg.sets.definitions.inv.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.conditions.WasKicked
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Verdeloth the Ancient
 * {4}{G}{G}
 * Legendary Creature — Treefolk
 * 4/7
 * Kicker {X} (You may pay an additional {X} as you cast this spell.)
 * Saproling creatures and other Treefolk creatures get +1/+1.
 * When Verdeloth enters, if it was kicked, create X 1/1 green Saproling creature tokens.
 *
 * Kicker {X} is a variable optional cost: the kicked cast prompts for X like a base-cost X
 * spell, and the chosen X flows through to the ETB trigger via [DynamicAmount.XValue] (the
 * spell's xValue is stamped onto the enters-the-battlefield event).
 *
 * The lord bonus is split into two static abilities mirroring the oracle wording — all
 * Saproling creatures, plus all *other* Treefolk creatures (excludeSelf so Verdeloth, a
 * Treefolk, doesn't pump itself).
 */
val VerdelothTheAncient = card("Verdeloth the Ancient") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Treefolk"
    power = 4
    toughness = 7
    oracleText = "Kicker {X} (You may pay an additional {X} as you cast this spell.)\n" +
        "Saproling creatures and other Treefolk creatures get +1/+1.\n" +
        "When Verdeloth enters, if it was kicked, create X 1/1 green Saproling creature tokens."

    keywordAbility(KeywordAbility.kicker("{X}"))

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Saproling"))
        )
    }

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Treefolk"), excludeSelf = true)
        )
    }

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = WasKicked
        effect = Effects.CreateToken(
            count = DynamicAmount.XValue,
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Saproling")
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "220"
        artist = "Daren Bader"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72d5fab1-fa20-4006-b19d-179d36238c9b.jpg?1562917985"
    }
}
