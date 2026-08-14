package com.wingedsheep.mtg.sets.definitions.rav.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Dimir Aqueduct
 * Land
 *
 * This land enters tapped.
 * When this land enters, return a land you control to its owner's hand.
 * {T}: Add {U}{B}.
 *
 * "Return a land you control" is technically non-targeted per oracle text, but the
 * engine models the controller's choice through a target requirement constrained to
 * their own lands — practically equivalent for a self-bounce that cannot fizzle.
 */
val DimirAqueduct = card("Dimir Aqueduct") {
    typeLine = "Land"
    colorIdentity = "UB"
    oracleText = "This land enters tapped.\n" +
        "When this land enters, return a land you control to its owner's hand.\n" +
        "{T}: Add {U}{B}."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val land = target("a land you control", TargetPermanent(filter = TargetFilter.Land.youControl()))
        effect = Effects.ReturnToHand(land)
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLUE).then(Effects.AddMana(Color.BLACK))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "276"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df3c3d56-8291-407e-87a1-94b7d12811fd.jpg?1783943591"
        ruling("2013-04-15", "If this land enters the battlefield and you control no other lands, its ability will force you to return it to your hand.")
    }
}
