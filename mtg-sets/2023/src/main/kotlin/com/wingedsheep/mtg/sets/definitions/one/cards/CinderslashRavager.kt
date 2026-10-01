package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Cinderslash Ravager
 * {4}{R}{G}
 * Creature — Phyrexian Warrior
 * 5/5
 *
 * This spell costs {1} less to cast for each permanent you control with oil counters on it.
 * Vigilance
 * When this creature enters, it deals 1 damage to each creature your opponents control.
 *
 * The reduction counts permanents (not counters) — two oil counters on one permanent still reduce
 * by only {1} — and only ever eats generic mana, so the floor is {R}{G}.
 */
val CinderslashRavager = card("Cinderslash Ravager") {
    manaCost = "{4}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Phyrexian Warrior"
    power = 5
    toughness = 5
    oracleText = "This spell costs {1} less to cast for each permanent you control with oil counters on it.\n" +
        "Vigilance\n" +
        "When this creature enters, it deals 1 damage to each creature your opponents control."

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.PermanentsWithCounterYouControl(GameObjectFilter.Permanent, CounterType.OIL)
            )
        )
    }

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Group.dealDamageToAll(1, GroupFilter.AllCreaturesOpponentsControl)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "200"
        artist = "Wisnu Tan"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/eee24300-fff8-4405-9629-9f62b4a839ef.jpg?1783918003"
    }
}
