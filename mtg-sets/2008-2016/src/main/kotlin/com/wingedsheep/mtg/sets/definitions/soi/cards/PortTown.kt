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
 * Port Town
 * Land
 *
 * As this land enters, you may reveal a Plains or Island card from your hand.
 * If you don't, this land enters tapped.
 * {T}: Add {W} or {U}.
 *
 * The SOI shadowland shape (see Choked Estuary): [OnEnterRun] wrapping
 * [Effects.MayRevealCardFromHand], whose `otherwise` rider taps the land.
 */
val PortTown = card("Port Town") {
    typeLine = "Land"
    colorIdentity = "WU"
    oracleText = "As this land enters, you may reveal a Plains or Island card from your hand. " +
        "If you don't, this land enters tapped.\n{T}: Add {W} or {U}."

    replacementEffect(
        OnEnterRun(
            Effects.MayRevealCardFromHand(
                filter = GameObjectFilter.Land.withAnySubtype("Plains", "Island"),
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
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "278"
        artist = "Noah Bradley"
        flavorText = "A haunted fog known as the Nebelgast shrouds the cities along Nephalia's coast."
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b866712a-c3ef-4a43-ac0f-146c7836f0d6.jpg?1783937697"
        ruling(
            "2016-04-08",
            "If an effect instructs you to put Port Town onto the battlefield tapped, it will still enter the " +
                "battlefield tapped even if you reveal a land card from your hand.",
        )
        ruling(
            "2016-04-08",
            "Lands don't have a subtype just because they can produce mana of the corresponding color. " +
                "Port Town itself is neither a Plains nor an Island, even though it produces white and blue " +
                "mana, so you can't reveal one to satisfy the ability of another.",
        )
        ruling(
            "2016-04-08",
            "You may reveal any land card with either or both of the appropriate subtypes. It doesn't have " +
                "to be a basic land. For example, you could reveal Canopy Vista from the Battle for Zendikar " +
                "set to satisfy the ability of Port Town.",
        )
        ruling(
            "2016-04-08",
            "If a Plains or Island is entering the battlefield from your hand at the same time as " +
                "Port Town, you may reveal the other land to have Port Town enter untapped.",
        )
    }
}
