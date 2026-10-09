package com.wingedsheep.mtg.sets.definitions.cmd.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Animar, Soul of Elements — Commander #181
 * {G}{U}{R} · Legendary Creature — Elemental · Mythic
 * 1/1
 *
 * Protection from white and from black
 * Whenever you cast a creature spell, put a +1/+1 counter on Animar.
 * Creature spells you cast cost {1} less to cast for each +1/+1 counter on Animar.
 *
 * The discount reads Animar's own +1/+1 counters at cast time
 * ([CostReductionSource.Dynamic] over [DynamicAmounts.countersOnSelf]) and only reduces generic
 * mana. The cast trigger fires after costs are paid, so its counter only discounts later spells.
 */
val AnimarSoulOfElements = card("Animar, Soul of Elements") {
    manaCost = "{G}{U}{R}"
    colorIdentity = "GUR"
    typeLine = "Legendary Creature — Elemental"
    power = 1
    toughness = 1
    oracleText = "Protection from white and from black\n" +
        "Whenever you cast a creature spell, put a +1/+1 counter on Animar.\n" +
        "Creature spells you cast cost {1} less to cast for each +1/+1 counter on Animar."

    keywordAbility(KeywordAbility.Protection(ProtectionScope.Colors(setOf(Color.WHITE, Color.BLACK))))

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Creature),
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.Dynamic(DynamicAmounts.countersOnSelf(CounterType.PLUS_ONE_PLUS_ONE))
            ),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "181"
        artist = "Peter Mohrbacher"
        imageUri = "https://cards.scryfall.io/normal/front/c/b/cb073d5b-9515-492d-9b2d-0f64e85f1da8.jpg?1783941186"
        ruling(
            "2018-03-16",
            "Animar's triggered ability triggers only when a creature spell is cast, after costs are paid. " +
                "The counter put on Animar for a creature spell won't affect the cost of that creature spell, " +
                "only future ones."
        )
        ruling(
            "2018-03-16",
            "Animar's triggered ability resolves before the creature spell that causes it to trigger. " +
                "The ability will resolve even if that spell is countered."
        )
    }
}
