package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Arcane Signet
 * {2}
 * Artifact
 *
 * {T}: Add one mana of any color in your commander's color identity.
 */
val ArcaneSignet = card("Arcane Signet") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{T}: Add one mana of any color in your commander's color identity."

    activatedAbility {
        cost = AbilityCost.Tap
        effect = Effects.AddManaOfColorInCommanderColorIdentity()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "331"
        artist = "Dan Murayama Scott"
        flavorText = "It started as a mere drop of water. The Magic Mirror crystallized it into much more."
        imageUri = "https://cards.scryfall.io/normal/front/8/4/84128e98-87d6-4c2f-909b-9435a7833e63.jpg?1783932545"
        ruling("2020-11-10", "If your commander is a card that has no colors in its color identity, Arcane Signet's ability produces no mana. It doesn't produce {C}.")
        ruling("2020-11-10", "If you have two commanders, the ability adds one mana of any color in their combined color identities.")
        ruling("2020-11-10", "If you don't have a commander, Arcane Signet's ability produces no mana.")
    }
}
