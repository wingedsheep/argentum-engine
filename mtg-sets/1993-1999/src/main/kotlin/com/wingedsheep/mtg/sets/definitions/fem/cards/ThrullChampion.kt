package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Thrull Champion
 * {4}{B}
 * Creature — Thrull
 * 2/2
 * Thrull creatures get +1/+1.
 * {T}: Gain control of target Thrull for as long as you control this creature.
 *
 * The lord pumps every Thrull on the battlefield, its own included, and it can steal a Thrull it
 * is itself pumping. The control effect is open-ended, bounded by
 * [Duration.WhileYouControlSource] rather than by end of turn.
 */
val ThrullChampion = card("Thrull Champion") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Thrull"
    oracleText = "Thrull creatures get +1/+1.\n" +
        "{T}: Gain control of target Thrull for as long as you control this creature."
    power = 2
    toughness = 2

    staticAbility {
        ability = ModifyStats(1, 1, GroupFilter(GameObjectFilter.Creature.withSubtype(Subtype.THRULL)))
    }

    activatedAbility {
        cost = Costs.Tap
        val t = target(
            "target Thrull",
            TargetPermanent(filter = TargetFilter(GameObjectFilter.Permanent.withSubtype(Subtype.THRULL)))
        )
        effect = Effects.GainControl(t, Duration.WhileYouControlSource("this creature"))
        description = "{T}: Gain control of target Thrull for as long as you control this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "44"
        artist = "Daniel Gelon"
        flavorText = "\"Those idiots should never have bred Thrulls for combat!\"\n—Jherana Rure"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4d3cafdd-a03b-4b08-b9c1-c776f8450d3a.jpg?1783947898"
    }
}
