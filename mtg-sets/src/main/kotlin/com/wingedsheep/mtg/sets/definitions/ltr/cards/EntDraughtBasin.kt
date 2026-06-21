package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Ent-Draught Basin
 * {2}
 * Artifact
 *
 * {X}, {T}: Put a +1/+1 counter on target creature with power X. Activate only as a sorcery.
 *
 * The "{X}" lives in the activated-ability cost (cf. Barad-dûr) — the X-cost activation
 * continuation threads the chosen X into [com.wingedsheep.sdk.scripting.values.DynamicAmount.XValue]
 * and onto the action. The target filter "creature with power X" references that same activation X:
 * [TargetFilter.powerEqualsX] (the power analogue of `manaValueEqualsX`, Void/Repeal) compares each
 * candidate's projected power against the chosen X. Legal-action enumeration runs before X is bound,
 * so the predicate matches permissively then; activation-time validation re-checks with X bound and
 * rejects any creature whose power isn't exactly X (a power-2 or power-4 creature can't be chosen for
 * X=3, only a power-3 one). `Activate only as a sorcery` is [TimingRule.SorcerySpeed].
 */
val EntDraughtBasin = card("Ent-Draught Basin") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "{X}, {T}: Put a +1/+1 counter on target creature with power X. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}"), Costs.Tap)
        target = TargetCreature(
            filter = TargetFilter.Creature.powerEqualsX(),
            id = "creature with power X"
        )
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0))
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "238"
        artist = "Torgeir Fjereide"
        flavorText = "The effect of the draught began at the toes, and rose steadily through every limb, " +
            "bringing refreshment and vigor as it coursed upward, right to the tips of the hair."
        imageUri = "https://cards.scryfall.io/normal/front/1/5/1500126f-fbe4-4c39-bb06-1a36e2c4682f.jpg?1686970148"
    }
}
