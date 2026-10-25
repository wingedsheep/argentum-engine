package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Hour of Revelation
 * {3}{W}{W}{W}
 * Sorcery
 * This spell costs {3} less to cast if there are ten or more nonland permanents on the battlefield.
 * Destroy all nonland permanents.
 *
 * The discount is a self-cast [ModifySpellCost] gated on a count of every nonland permanent on the
 * battlefield ([Player.Each]), evaluated as the total cost is determined (CR 601.2f) — before any of
 * it is paid, so sacrificing permanents for mana afterwards doesn't undo it (2017-07-14 ruling).
 * The effect is Planar Cleansing's destroy-all pipeline.
 */
val HourOfRevelation = card("Hour of Revelation") {
    manaCost = "{3}{W}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "This spell costs {3} less to cast if there are ten or more nonland permanents on the " +
        "battlefield.\nDestroy all nonland permanents."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGeneric(3),
            gating = CostGating.OnlyIf(
                Conditions.CompareAmounts(
                    DynamicAmounts.battlefield(Player.Each, GameObjectFilter.NonlandPermanent).count(),
                    ComparisonOperator.GTE,
                    10,
                ),
            ),
        )
    }

    spell {
        effect = Patterns.Group.destroyAllPipeline(GameObjectFilter.NonlandPermanent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "15"
        artist = "Raymond Swanland"
        flavorText = "In the first moments, the gate to the afterlife opened, and the people were prostrate " +
            "in the light of the God-Pharaoh."
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89a89652-95ef-45b4-80c3-02241483f3c0.jpg?1783936060"
        ruling(
            "2017-07-14",
            "You determine the cost to cast Hour of Revelation before you pay any of that cost. For example, " +
                "if the only nonland permanents on the battlefield are Tezzeret the Schemer and nine Etherium " +
                "Cells he's given you, Hour of Revelation costs {W}{W}{W} to cast. You may sacrifice three " +
                "Etherium Cells to pay this cost, even though there are no longer ten or more nonland " +
                "permanents after you produce {W}{W}{W}."
        )
    }
}
