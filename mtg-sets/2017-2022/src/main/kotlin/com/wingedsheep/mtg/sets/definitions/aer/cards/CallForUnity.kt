package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Call for Unity — Aether Revolt #9
 * {3}{W}{W}
 * Enchantment
 *
 * Revolt — At the beginning of your end step, if a permanent left the battlefield under your
 * control this turn, put a unity counter on this enchantment.
 * Creatures you control get +1/+1 for each unity counter on this enchantment.
 *
 * Revolt is a CR 603.4 intervening-if, checked when the end step begins and again on resolution,
 * so it rides on `interveningIf` with the per-player
 * [Conditions.YouHadPermanentLeaveBattlefieldThisTurn] tracker. The anthem is a live static
 * ability sized by the [CounterType.UNITY] counters currently on the enchantment.
 *
 * Aether Revolt is the earliest printing; the Tales of Middle-earth Commander (LTC) reprint is a
 * `Printing` row in that set.
 */
val CallForUnity = card("Call for Unity") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "Revolt — At the beginning of your end step, if a permanent left the battlefield " +
        "under your control this turn, put a unity counter on this enchantment.\n" +
        "Creatures you control get +1/+1 for each unity counter on this enchantment."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.YouHadPermanentLeaveBattlefieldThisTurn
        effect = Effects.AddCounters(CounterType.UNITY, 1, EffectTarget.Self)
        description = "Revolt — At the beginning of your end step, if a permanent left the battlefield " +
            "under your control this turn, put a unity counter on this enchantment."
    }

    staticAbility {
        val unity = DynamicAmounts.countersOnSelf(CounterType.UNITY)
        ability = GrantDynamicStats(
            filter = GroupFilter.AllCreaturesYouControl,
            powerBonus = unity,
            toughnessBonus = unity
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "9"
        artist = "John Severin Brassell"
        flavorText = "The \"leaking spire\" stands for liberation from Consulate control."
        imageUri = "https://cards.scryfall.io/normal/front/e/e/eeca4557-98aa-433b-a3ee-050e4a3e6d88.jpg?1783936781"
        ruling("2017-02-09", "Revolt abilities check only whether a permanent you controlled left the battlefield this turn or not. They don't apply multiple times if more than one permanent you controlled left the battlefield. They don't check whether the permanent that left the battlefield is still in the zone it moved to.")
        ruling("2017-02-09", "Revolt abilities don't care why the permanent left the battlefield, who caused it to move, or where it moved to. They're equally satisfied by an artifact you sacrificed to pay a cost, a creature you controlled that was destroyed by Murder, or an enchantment you returned to your hand with Leave in the Dust.")
        ruling("2017-02-09", "Tokens that leave the battlefield will satisfy a revolt ability.")
        ruling("2017-02-09", "Energy counters aren't permanents. Paying {E} won't satisfy a revolt ability.")
        ruling("2017-02-09", "All cards in the Aether Revolt set with triggered revolt abilities use an intervening \"if\" clause. A permanent you controlled must have left the battlefield earlier in the turn in order for these abilities to trigger; otherwise they do nothing.")
    }
}
