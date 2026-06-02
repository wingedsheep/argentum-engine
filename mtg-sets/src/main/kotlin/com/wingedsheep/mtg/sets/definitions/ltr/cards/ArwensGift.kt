package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.conditions.Compare
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Arwen's Gift
 * {3}{U}
 * Sorcery
 *
 * This spell costs {1} less to cast if you control two or more legendary creatures.
 * Scry 2, then draw two cards.
 */
val ArwensGift = card("Arwen's Gift") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "This spell costs {1} less to cast if you control two or more legendary creatures.\n" +
        "Scry 2, then draw two cards."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(1),
            gating = CostGating.OnlyIf(
                Compare(
                    left = DynamicAmount.AggregateBattlefield(
                        player = Player.You,
                        filter = GameObjectFilter.Creature.legendary()
                    ),
                    operator = ComparisonOperator.GTE,
                    right = DynamicAmount.Fixed(2)
                )
            )
        )
    }

    spell {
        effect = LibraryPatterns.scry(2).then(Effects.DrawCards(2))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Wangjie Li"
        flavorText = "\"Wear this now in memory of Elfstone and Evenstar with whom your life has been woven!\""
        imageUri = "https://cards.scryfall.io/normal/front/3/0/30e1ec49-ad4f-4623-aeeb-dba07d6e6251.jpg?1686967996"
    }
}
