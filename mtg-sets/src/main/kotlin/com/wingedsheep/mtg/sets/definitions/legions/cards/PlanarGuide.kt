package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Planar Guide
 * {W}
 * Creature — Human Cleric
 * 1/1
 * {3}{W}, Exile Planar Guide: Exile all creatures. At the beginning of the next end step,
 * return those cards to the battlefield under their owners' control.
 */
val PlanarGuide = card("Planar Guide") {
    manaCost = "{W}"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 1
    oracleText = "{3}{W}, Exile Planar Guide: Exile all creatures. At the beginning of the next end step, return those cards to the battlefield under their owners' control."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{3}{W}"), Costs.ExileSelf)
        effect = Effects.ExileGroupAndLink(GroupFilter.AllCreatures)
            .then(
                CreateDelayedTriggerEffect(
                    step = Step.END,
                    effect = Effects.ReturnLinkedExileUnderOwnersControl()
                )
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "Eric Peterson"
        flavorText = "\"Every moment has its own savior.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/0/7087cb1e-f2e2-4b75-bacf-bc4153e398e3.jpg?1562917504"
        ruling("2004-10-04", "All 'enters' abilities trigger as normal.")
        ruling("2004-10-04", "Creatures that were face down return to the battlefield face up.")
        ruling("2004-10-04", "If the ability is activated during the End step, then the creatures do not return until the End step of the following turn.")
        ruling("2005-08-01", "When the creatures leave the battlefield, all Auras on them go to their owners' graveyards, any counters on them are removed, and effects on them end.")
    }
}
