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
 * Foreboding Ruins
 * Land
 *
 * As this land enters, you may reveal a Swamp or Mountain card from your hand.
 * If you don't, this land enters tapped.
 * {T}: Add {B} or {R}.
 *
 * Same shape as the rest of the SOI shadowland cycle (see Game Trail):
 * [OnEnterRun] wrapping [Effects.MayRevealCardFromHand] with a tap rider.
 */
val ForebodingRuins = card("Foreboding Ruins") {
    typeLine = "Land"
    colorIdentity = "BR"
    oracleText = "As this land enters, you may reveal a Swamp or Mountain card from your hand. " +
        "If you don't, this land enters tapped.\n{T}: Add {B} or {R}."

    replacementEffect(
        OnEnterRun(
            Effects.MayRevealCardFromHand(
                filter = GameObjectFilter.Land.withAnySubtype("Swamp", "Mountain"),
                otherwise = Effects.Tap(EffectTarget.Self),
            )
        )
    )

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "272"
        artist = "Adam Paquette"
        flavorText = "Sent to find Sorin, Jace ventured to the vampire's ancestral home, only to find evidence of a deeper mystery."
        imageUri = "https://cards.scryfall.io/normal/front/3/c/3c26ee31-7e2a-4eed-b448-04989fb57523.jpg?1783937700"
        ruling(
            "2016-04-08",
            "Lands don't have a subtype just because they can produce mana of the corresponding color. " +
                "Foreboding Ruins itself is neither a Swamp nor a Mountain, even though it produces black and red " +
                "mana, so you can't reveal one to satisfy the ability of another.",
        )
        ruling(
            "2016-04-08",
            "If an effect instructs you to put Foreboding Ruins onto the battlefield tapped, it will still " +
                "enter the battlefield tapped even if you reveal a land card from your hand.",
        )
        ruling(
            "2016-04-08",
            "You may reveal any land card with either or both of the appropriate subtypes. It doesn't have " +
                "to be a basic land. For example, you could reveal Sunken Hollow from the Battle for Zendikar " +
                "set to satisfy the ability of Foreboding Ruins.",
        )
        ruling(
            "2016-04-08",
            "If a Swamp or Mountain is entering the battlefield from your hand at the same time as " +
                "Foreboding Ruins, you may reveal the other land to have Foreboding Ruins enter untapped.",
        )
    }
}
