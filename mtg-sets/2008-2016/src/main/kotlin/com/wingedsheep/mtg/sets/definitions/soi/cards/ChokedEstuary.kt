package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.OnEnterRun
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Choked Estuary
 * Land
 *
 * As this land enters, you may reveal an Island or Swamp card from your hand.
 * If you don't, this land enters tapped.
 * {T}: Add {U} or {B}.
 *
 * Composed from two atoms:
 *  - [OnEnterRun] — generic "as ~ enters, run [effect]" replacement wrapper.
 *  - [Effects.MayRevealCardFromHand] — atomic optional reveal with an `otherwise`
 *    rider that fires when the player declines or has no eligible card. Here the
 *    rider taps the land, expressing "if you don't, this land enters tapped."
 *
 * The whole SOI shadowland cycle (Choked Estuary, Foreboding Ruins, Fortified
 * Village, Game Trail, Port Town) reuses this exact shape — only the filter
 * subtypes and produced mana differ.
 */
val ChokedEstuary = card("Choked Estuary") {
    typeLine = "Land"
    colorIdentity = "UB"
    oracleText = "As this land enters, you may reveal an Island or Swamp card from your hand. " +
        "If you don't, this land enters tapped.\n{T}: Add {U} or {B}."

    replacementEffect(
        OnEnterRun(
            Effects.MayRevealCardFromHand(
                filter = GameObjectFilter.Land.withAnySubtype("Island", "Swamp"),
                otherwise = Effects.Tap(EffectTarget.Self),
            )
        )
    )

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "270"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/995d44ca-626d-4c95-97af-ee53fa8baaf0.jpg?1783937700"
        ruling(
            "2016-04-08",
            "Lands don't have a subtype just because they can produce mana of the corresponding color. " +
                "Choked Estuary itself is neither an Island nor a Swamp, even though it produces blue and black " +
                "mana, so you can't reveal one to satisfy the ability of another.",
        )
        ruling(
            "2016-04-08",
            "If an effect instructs you to put Choked Estuary onto the battlefield tapped, it will still " +
                "enter the battlefield tapped even if you reveal a land card from your hand.",
        )
        ruling(
            "2016-04-08",
            "You may reveal any land card with either or both of the appropriate subtypes. It doesn't have " +
                "to be a basic land. For example, you could reveal Prairie Stream from the Battle for Zendikar " +
                "set to satisfy the ability of Choked Estuary.",
        )
        ruling(
            "2016-04-08",
            "If an Island or Swamp is entering the battlefield from your hand at the same time as " +
                "Choked Estuary, you may reveal the other land to have Choked Estuary enter untapped.",
        )
    }
}
