package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Kelpie Guide — Strixhaven: School of Mages #45 (canonical printing)
 * {2}{U} · Creature — Beast · 2/2
 *
 * {T}: Untap another target permanent you control.
 * {T}: Tap target permanent. Activate only if you control eight or more lands.
 *
 * Two plain tap abilities. The first is Forensic Researcher's "untap another target permanent you
 * control" (`excludeSelf`); the second gates [Effects.Tap] behind an
 * [ActivationRestriction.OnlyIfCondition] on [Conditions.ControlLandsAtLeast] (Temple of the False
 * God's shape), checked as the ability is activated.
 */
val KelpieGuide = card("Kelpie Guide") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Beast"
    power = 2
    toughness = 2
    oracleText = "{T}: Untap another target permanent you control.\n" +
        "{T}: Tap target permanent. Activate only if you control eight or more lands."

    activatedAbility {
        cost = Costs.Tap
        val permanent = target(TargetFilter.Permanent.youControl().copy(excludeSelf = true))
        effect = Effects.Untap(permanent)
        description = "Untap another target permanent you control."
    }

    activatedAbility {
        cost = Costs.Tap
        val permanent = target(TargetFilter.Permanent)
        effect = Effects.Tap(permanent)
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.ControlLandsAtLeast(8)))
        description = "Tap target permanent. Activate only if you control eight or more lands."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "45"
        artist = "Ilse Gort"
        flavorText = "Kelpies are drawn to paradoxes, making them ideal companions for Quandrix mages."
        imageUri = "https://cards.scryfall.io/normal/front/0/1/0112ebfb-55ad-401c-9dc5-ffd829f5b5bf.jpg?1783927379"
    }
}
