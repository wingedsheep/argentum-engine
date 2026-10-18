package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.div
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fireball — Limited Edition Alpha #149
 * {X}{R} · Sorcery
 *
 * This spell costs {1} more to cast for each target beyond the first.
 * Fireball deals X damage divided evenly, rounded down, among any number of targets.
 *
 * *The tax* is the generic sibling of Officious Interrogation's: a self-cast [ModifySpellCost]
 * whose count source is [CostReductionSource.ChosenTargets]. It is a cost increase,
 * so a free cast still owes it (CR 601.2f), and it never touches the mana value.
 *
 * *The division* happens on resolution with no choices: each target still legal then takes
 * floor(X / N), N being how many targets are still legal. The resolver hands the effect only the
 * still-legal targets (CR 608.2b), so `targetCount()` read *before* the per-target loop is exactly N.
 * It must be frozen with `StoreNumber` first — inside `ForEachTarget` the context holds only the one
 * target being visited, so a count read there would always be 1. With more legal targets than X
 * the share is 0 and nothing is dealt damage. With zero targets the spell isn't targeted at all and
 * simply resolves doing nothing (the share is 0 — a division by zero reads 0).
 */
val Fireball = card("Fireball") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "This spell costs {1} more to cast for each target beyond the first.\n" +
        "Fireball deals X damage divided evenly, rounded down, among any number of targets."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.IncreaseGenericBy(CostReductionSource.ChosenTargets(beyond = 1))
        )
    }

    spell {
        target(Targets.AnyNumber)
        effect = Effects.StoreNumber(
            "share",
            DynamicAmounts.xValue() / DynamicAmounts.targetCount()
        ) then Effects.ForEachTarget(
            Effects.DealDamage(DynamicAmounts.storedNumber("share"), EffectTarget.ContextTarget(0))
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "149"
        artist = "Mark Tedin"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7623c00-144b-4a8f-9c6c-f5e9e4f65ece.jpg?1783948686"

        ruling(
            "2017-11-17",
            "For example, if you choose three targets and X is 5, Fireball costs {7}{R} to cast. " +
                "If all three targets are still legal as Fireball resolves, it deals 1 damage to each of them."
        )
        ruling(
            "2017-11-17",
            "You may cast Fireball with zero targets, regardless of the value you choose for X. " +
                "If you do, it's not a targeted spell and won't deal any damage."
        )
        ruling(
            "2017-11-17",
            "Fireball's damage is divided as Fireball resolves, not as it's cast, because there " +
                "are no choices involved. The division takes into account only targets that are still " +
                "legal at that time."
        )
        ruling(
            "2017-11-17",
            "You can choose more targets than X. If the number of legal targets as Fireball " +
                "resolves is greater than X, none of them are dealt damage."
        )
    }
}
