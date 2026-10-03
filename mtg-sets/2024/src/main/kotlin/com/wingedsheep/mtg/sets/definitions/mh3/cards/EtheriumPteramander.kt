package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Etherium Pteramander {B}
 * Artifact Creature — Salamander Drake
 * 1/1
 * Flying
 * This creature can block only creatures with flying.
 * {6}{B}: Adapt 4. This ability costs {1} less to activate for each other artifact you control.
 *
 * Adapt is the zero-counter gate over AddCounters (CR 701.46a); the cost reduction is
 * `genericCostReduction` over a count of *other* artifacts you control, so {B} always survives.
 */
val EtheriumPteramander = card("Etherium Pteramander") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Salamander Drake"
    oracleText = "Flying\n" +
        "This creature can block only creatures with flying.\n" +
        "{6}{B}: Adapt 4. This ability costs {1} less to activate for each other artifact you control. " +
        "(If this creature has no +1/+1 counters on it, put four +1/+1 counters on it.)"
    power = 1
    toughness = 1

    keywords(Keyword.FLYING)

    staticAbility {
        ability = CanOnlyBlockCreaturesWith(blockerFilter = GameObjectFilter.Creature.withKeyword(Keyword.FLYING))
    }

    activatedAbility {
        cost = Costs.Mana("{6}{B}")
        genericCostReduction = DynamicAmounts.battlefield(
            Player.You,
            GameObjectFilter.Artifact,
            excludeSelf = true,
        ).count()
        effect = Effects.If(
            Conditions.Not(Conditions.SourceHasCounter(CounterType.PLUS_ONE_PLUS_ONE)),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 4, EffectTarget.Self),
        )
        description = "{6}{B}: Adapt 4. This ability costs {1} less to activate for each other artifact you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "92"
        artist = "Kekai Kotaki"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3abd093b-c1d6-400e-bc26-1aa045fe47b8.jpg?1783911281"
        ruling(
            "2024-06-07",
            "You can always activate an ability that will cause a creature to adapt. As that ability resolves, " +
                "if the creature has a +1/+1 counter on it for any reason, you simply won't put any +1/+1 counters on it.",
        )
    }
}
