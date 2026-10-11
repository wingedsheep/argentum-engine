package com.wingedsheep.mtg.sets.definitions.stx.cards

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
 * Frostboil Snarl
 * Land
 *
 * As this land enters, you may reveal an Island or Mountain card from your hand.
 * If you don't, this land enters tapped.
 * {T}: Add {U} or {R}.
 *
 * Strixhaven's "Snarl" cycle is the Shadows over Innistrad shadowland shape (see Game Trail):
 * [OnEnterRun] wrapping [Effects.MayRevealCardFromHand], whose `otherwise` rider taps the land
 * when the player declines or holds no Island/Mountain land card.
 */
val FrostboilSnarl = card("Frostboil Snarl") {
    typeLine = "Land"
    colorIdentity = "UR"
    oracleText = "As this land enters, you may reveal an Island or Mountain card from your hand. " +
        "If you don't, this land enters tapped.\n{T}: Add {U} or {R}."

    replacementEffect(
        OnEnterRun(
            Effects.MayRevealCardFromHand(
                filter = GameObjectFilter.Land.withAnySubtype("Island", "Mountain"),
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
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "265"
        artist = "Sam Burley"
        flavorText = "A smoldering storm of chilling fire."
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c22ba565-da08-4957-93bb-8d266fe0b0d8.jpg?1783927278"
        ruling(
            "2021-04-16",
            "The \"Snarl\" itself doesn't have any land subtypes. You can't reveal one to satisfy the ability of another.",
        )
        ruling(
            "2021-04-16",
            "If a land card with an appropriate subtype is entering the battlefield from your hand at the same time " +
                "as one of these lands, you may reveal the other land to have the \"Snarl\" enter untapped.",
        )
        ruling(
            "2021-04-16",
            "If an effect instructs you to put one of these lands onto the battlefield tapped, it will still enter " +
                "the battlefield tapped even if you reveal a land card from your hand.",
        )
        ruling(
            "2021-04-16",
            "You may reveal any land card with either or both of the appropriate subtypes. It doesn't have to be a " +
                "basic land card.",
        )
    }
}
