package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sentry Bot
 * {4}{W}
 * Artifact Creature — Robot
 * 2/5
 *
 * Flash
 * This spell costs {1} less to cast for each creature attacking you.
 * When this creature enters, you get {E} for each creature attacking you.
 * At the beginning of combat on your turn, you may pay {E}{E}{E}. If you do, put a +1/+1 counter
 * on each creature you control.
 *
 * "Creature attacking you" is every creature on the battlefield whose declared defender is you
 * yourself — `attackingPlayer(EffectTarget.Controller)` counted across all players — so in
 * multiplayer a creature attacking another opponent, or attacking one of your planeswalkers,
 * doesn't count. The cost reduction is a self-cast [CostReductionSource.Dynamic] (Deem Inferior's
 * shape), where "you" is the caster; the energy count is read as the enters trigger resolves
 * (ruling). The combat trigger is an [Effects.MayPay] over exactly three energy (Robobrain War
 * Mind's shape) gating a group counter placement.
 */
val SentryBot = card("Sentry Bot") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Artifact Creature — Robot"
    power = 2
    toughness = 5
    oracleText = "Flash\n" +
        "This spell costs {1} less to cast for each creature attacking you.\n" +
        "When this creature enters, you get {E} for each creature attacking you.\n" +
        "At the beginning of combat on your turn, you may pay {E}{E}{E}. If you do, put a +1/+1 " +
        "counter on each creature you control."

    val creaturesAttackingYou = DynamicAmounts.battlefield(
        Player.Each,
        GameObjectFilter.Creature.attackingPlayer(EffectTarget.Controller),
    ).count()

    keywords(Keyword.FLASH)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(creaturesAttackingYou),
            ),
        )
    }

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.AddDynamicCounters(CounterType.ENERGY, creaturesAttackingYou, EffectTarget.Controller)
        description = "When this creature enters, you get {E} for each creature attacking you."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        effect = Effects.MayPay(
            cost = Effects.PayExactCounters(CounterType.ENERGY, 3),
            then = Effects.ForEachInGroup(
                GroupFilter(GameObjectFilter.Creature.youControl()),
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.IterationEntity),
            ),
        )
        description = "At the beginning of combat on your turn, you may pay {E}{E}{E}. If you do, " +
            "put a +1/+1 counter on each creature you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "24"
        artist = "Anthony Devine"
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b16b0143-3511-4f79-bdb1-1ff7af55f051.jpg?1783912415"
        ruling(
            "2024-03-08",
            "The cost-reduction ability of Sentry Bot doesn't change its mana cost or mana value, only " +
                "the total cost you pay. Specifically, the mana value of Sentry Bot is always 5."
        )
        ruling(
            "2024-03-08",
            "Once you announce that you're casting Sentry Bot, no player may take actions until you're " +
                "done casting it. Notably, opponents can't remove creatures from combat to try to change " +
                "Sentry Bot's total cost."
        )
        ruling(
            "2024-03-08",
            "Use the number of creatures attacking you as Sentry Bot's third ability resolves to " +
                "determine the number of energy counters you get."
        )
    }
}
