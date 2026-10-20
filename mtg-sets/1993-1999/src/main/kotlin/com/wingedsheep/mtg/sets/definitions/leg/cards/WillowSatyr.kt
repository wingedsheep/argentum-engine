package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Willow Satyr
 * {2}{G}{G}
 * Creature — Satyr
 * 1/1
 * You may choose not to untap this creature during your untap step.
 * {T}: Gain control of target legendary creature for as long as you control this creature and
 * this creature remains tapped.
 *
 * Seasinger's engine over a legendary-creature target: `AbilityFlag.MAY_NOT_UNTAP` for the optional
 * untap, and [Duration.WhileYouControlSourceAndSourceTapped] for the two-part duration — untapping
 * the Satyr, losing control of it, or it leaving the battlefield all hand the creature back.
 */
val WillowSatyr = card("Willow Satyr") {
    manaCost = "{2}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Satyr"
    oracleText = "You may choose not to untap this creature during your untap step.\n" +
        "{T}: Gain control of target legendary creature for as long as you control this creature " +
        "and this creature remains tapped."
    power = 1
    toughness = 1

    flags(AbilityFlag.MAY_NOT_UNTAP)

    activatedAbility {
        cost = Costs.Tap
        val t = target(TargetFilter(GameObjectFilter.Creature.legendary()))
        effect = Effects.GainControl(
            t,
            Duration.WhileYouControlSourceAndSourceTapped("Willow Satyr"),
        )
        description = "{T}: Gain control of target legendary creature for as long as you control " +
            "this creature and this creature remains tapped."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "212"
        artist = "Jeff A. Menges"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c8b1f49-550e-405f-b17c-1d94589494ad.jpg?1783948043"
        ruling("2009-10-01", "If Willow Satyr leaves the battlefield, you no longer control it, so the duration of its control-change effect ends.")
        ruling("2009-10-01", "If Willow Satyr stops being tapped before its ability resolves — even if it becomes tapped again right away — you won't gain control of the targeted creature at all. The same is true if you lose control of Willow Satyr before the ability resolves.")
        ruling("2009-10-01", "Once the ability resolves, it doesn't care whether the targeted permanent remains a legal target for the ability, only that it's on the battlefield. The effect will continue to apply to it even if it gains shroud, stops being a creature, stops being legendary, or would no longer be a legal target for any other reason.")
    }
}
