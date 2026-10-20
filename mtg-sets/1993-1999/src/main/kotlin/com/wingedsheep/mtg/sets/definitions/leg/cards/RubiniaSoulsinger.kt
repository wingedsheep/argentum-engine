package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Rubinia Soulsinger
 * {2}{G}{W}{U}
 * Legendary Creature — Faerie
 * 2/3
 * You may choose not to untap Rubinia Soulsinger during your untap step.
 * {T}: Gain control of target creature for as long as you control Rubinia Soulsinger and
 * Rubinia Soulsinger remains tapped.
 *
 * Seasinger / Willow Satyr's engine over any target creature: `AbilityFlag.MAY_NOT_UNTAP` for the
 * optional untap, and [Duration.WhileYouControlSourceAndSourceTapped] for the two-part duration.
 */
val RubiniaSoulsinger = card("Rubinia Soulsinger") {
    manaCost = "{2}{G}{W}{U}"
    colorIdentity = "GUW"
    typeLine = "Legendary Creature — Faerie"
    oracleText = "You may choose not to untap Rubinia Soulsinger during your untap step.\n" +
        "{T}: Gain control of target creature for as long as you control Rubinia Soulsinger " +
        "and Rubinia Soulsinger remains tapped."
    power = 2
    toughness = 3

    flags(AbilityFlag.MAY_NOT_UNTAP)

    activatedAbility {
        cost = Costs.Tap
        val t = target(TargetFilter.Creature)
        effect = Effects.GainControl(
            t,
            Duration.WhileYouControlSourceAndSourceTapped("Rubinia Soulsinger"),
        )
        description = "{T}: Gain control of target creature for as long as you control Rubinia " +
            "Soulsinger and Rubinia Soulsinger remains tapped."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "256"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/f/1/f13e8dc9-8d0f-4a2c-8c0e-be70a3a7dc8e.jpg?1783948033"
        ruling("2009-10-01", "If Rubinia Soulsinger leaves the battlefield, you no longer control it, so the duration of its control-change effect ends.")
        ruling("2009-10-01", "If Rubinia Soulsinger stops being tapped before its ability resolves — even if it becomes tapped again right away — you won't gain control of the targeted creature at all. The same is true if you lose control of Rubinia Soulsinger before the ability resolves.")
        ruling("2009-10-01", "Once the ability resolves, it doesn't care whether the targeted permanent remains a legal target for the ability, only that it's on the battlefield. The effect will continue to apply to it even if it gains shroud, stops being a creature, or would no longer be a legal target for any other reason.")
    }
}
