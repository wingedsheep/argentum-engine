package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Goldfury Strider
 * {4}{R}
 * Artifact Creature — Golem
 * 3/5
 * Trample
 * Tap two untapped artifacts and/or creatures you control: Target creature gets +2/+0 until end
 * of turn. Activate only as a sorcery.
 *
 * The activation cost reuses the [Costs.TapPermanents] primitive (same shape as Adaptive
 * Gemguard), requiring exactly two untapped artifacts or creatures you control. The effect
 * applies a temporary power boost via [Effects.ModifyStats], referencing the declared target
 * with [EffectTarget.ContextTarget].
 */
val GoldfuryStrider = card("Goldfury Strider") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Golem"
    power = 3
    toughness = 5
    oracleText = "Trample\nTap two untapped artifacts and/or creatures you control: Target creature gets +2/+0 until end of turn. Activate only as a sorcery."

    keywords(Keyword.TRAMPLE)

    activatedAbility {
        cost = Costs.TapPermanents(
            count = 2,
            filter = GameObjectFilter.Artifact or GameObjectFilter.Creature,
        )
        target = Targets.Creature
        effect = Effects.ModifyStats(2, 0, EffectTarget.ContextTarget(0))
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "152"
        artist = "José Parodi"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/415904fe-b77f-4c1a-850f-688484d629e6.jpg?1782694487"
    }
}
