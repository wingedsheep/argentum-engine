package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.OnEnterRun
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Shineshadow Snarl — Strixhaven: School of Mages #272 (canonical printing)
 * Land
 *
 * As this land enters, you may reveal a Plains or Swamp card from your hand.
 * If you don't, this land enters tapped.
 * {T}: Add {W} or {B}.
 *
 * Strixhaven's "Snarl" cycle is the Shadows over Innistrad shadowland shape (see Game Trail):
 * [OnEnterRun] wrapping [Effects.MayRevealCardFromHand], whose `otherwise` rider taps the land
 * when the player declines or holds no Plains/Swamp land card.
 */
val ShineshadowSnarl = card("Shineshadow Snarl") {
    typeLine = "Land"
    colorIdentity = "WB"
    oracleText = "As this land enters, you may reveal a Plains or Swamp card from your hand. " +
        "If you don't, this land enters tapped.\n{T}: Add {W} or {B}."

    replacementEffect(
        OnEnterRun(
            Effects.MayRevealCardFromHand(
                filter = GameObjectFilter.Land.withAnySubtype("Plains", "Swamp"),
                otherwise = Effects.EnterTapped(),
            )
        )
    )

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.WHITE)
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
        collectorNumber = "272"
        artist = "Sam Burley"
        flavorText = "A flickering beacon of righteous corruption."
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84a46ebf-6938-4100-8f5f-0605ad2be4fc.jpg?1783927270"
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
