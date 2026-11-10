package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.OnEnterRun
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Fortified Village
 * Land
 *
 * As this land enters, you may reveal a Forest or Plains card from your hand.
 * If you don't, this land enters tapped.
 * {T}: Add {G} or {W}.
 *
 * Same shape as the rest of the SOI shadowland cycle (see [ChokedEstuary]):
 * [OnEnterRun] wrapping [Effects.MayRevealCardFromHand], whose `otherwise`
 * rider taps the land when the player declines or has no Forest/Plains card.
 */
val FortifiedVillage = card("Fortified Village") {
    typeLine = "Land"
    colorIdentity = "GW"
    oracleText = "As this land enters, you may reveal a Forest or Plains card from your hand. " +
        "If you don't, this land enters tapped.\n{T}: Add {G} or {W}."

    replacementEffect(
        OnEnterRun(
            Effects.MayRevealCardFromHand(
                filter = GameObjectFilter.Land.withAnySubtype("Forest", "Plains"),
                otherwise = Effects.EnterTapped(),
            )
        )
    )

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "274"
        artist = "Cliff Childs"
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1feb9dc1-671d-43ad-ae22-ed1a9916b140.jpg?1783937698"
        ruling(
            "2016-04-08",
            "Lands don't have a subtype just because they can produce mana of the corresponding color. " +
                "Fortified Village itself is neither a Forest nor a Plains, even though it produces green and " +
                "white mana, so you can't reveal one to satisfy the ability of another.",
        )
        ruling(
            "2016-04-08",
            "If an effect instructs you to put Fortified Village onto the battlefield tapped, it will still " +
                "enter the battlefield tapped even if you reveal a land card from your hand.",
        )
        ruling(
            "2016-04-08",
            "You may reveal any land card with either or both of the appropriate subtypes. It doesn't have " +
                "to be a basic land. For example, you could reveal Prairie Stream from the Battle for Zendikar " +
                "set to satisfy the ability of Fortified Village.",
        )
        ruling(
            "2016-04-08",
            "If a Forest or Plains is entering the battlefield from your hand at the same time as " +
                "Fortified Village, you may reveal the other land to have Fortified Village enter untapped.",
        )
    }
}
