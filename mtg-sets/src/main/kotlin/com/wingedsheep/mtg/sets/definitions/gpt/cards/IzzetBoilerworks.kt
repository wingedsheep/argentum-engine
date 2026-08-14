package com.wingedsheep.mtg.sets.definitions.gpt.cards

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
 * Izzet Boilerworks
 * Land
 *
 * This land enters tapped.
 * When this land enters, return a land you control to its owner's hand.
 * {T}: Add {U}{R}.
 *
 * "Return a land you control" is technically non-targeted per oracle text, but the
 * engine models the controller's choice through a target requirement constrained to
 * their own lands — practically equivalent for a self-bounce that cannot fizzle.
 */
val IzzetBoilerworks = card("Izzet Boilerworks") {
    typeLine = "Land"
    colorIdentity = "UR"
    oracleText = "This land enters tapped.\n" +
        "When this land enters, return a land you control to its owner's hand.\n" +
        "{T}: Add {U}{R}."

    replacementEffect(EntersTapped())

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val land = target("a land you control", TargetPermanent(filter = TargetFilter.Land.youControl()))
        effect = Effects.ReturnToHand(land)
    }

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddMana(Color.BLUE).then(Effects.AddMana(Color.RED))
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "159"
        artist = "John Avon"
        imageUri = "https://cards.scryfall.io/normal/front/6/6/666f455e-3a3d-475d-b67a-a1fdd74820eb.jpg?1783943456"
        ruling("2013-04-15", "If this land enters the battlefield and you control no other lands, its ability will force you to return it to your hand.")
    }
}
