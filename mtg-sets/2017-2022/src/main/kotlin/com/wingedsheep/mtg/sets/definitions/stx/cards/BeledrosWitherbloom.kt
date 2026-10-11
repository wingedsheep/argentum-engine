package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Beledros Witherbloom — Strixhaven: School of Mages #163 (canonical printing)
 * {5}{B}{G} · Legendary Creature — Elder Dragon · 4/4
 *
 * Flying
 * At the beginning of each upkeep, create a 1/1 black and green Pest creature token with
 * "When this token dies, you gain 1 life."
 * Pay 10 life: Untap all lands you control. Activate only once each turn.
 *
 * "Each upkeep" is every player's upkeep (`Triggers.anyPlayer`). The Pest is the predefined
 * Witherbloom token (`Effects.CreatePest()`), and the untap is the Wilderness Reclamation shape.
 */
val BeledrosWitherbloom = card("Beledros Witherbloom") {
    manaCost = "{5}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Elder Dragon"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "At the beginning of each upkeep, create a 1/1 black and green Pest creature token with " +
        "\"When this token dies, you gain 1 life.\"\n" +
        "Pay 10 life: Untap all lands you control. Activate only once each turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        effect = Effects.CreatePest()
    }

    activatedAbility {
        cost = Costs.PayLife(10)
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Land.youControl()),
            Effects.Untap(EffectTarget.IterationEntity)
        )
        description = "Pay 10 life: Untap all lands you control. Activate only once each turn."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "163"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/93f786a2-a7b7-4f9e-92e8-9a6a11efe290.jpg?1783927324"
    }
}
