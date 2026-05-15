package com.wingedsheep.mtg.sets.definitions.blc.cards

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
        collectorNumber = "127"
        artist = "Ioannis Fiore"
        flavorText = "The dark nights of Valley hold no horrors for batfolk clerics and warriors."
        imageUri = "https://cards.scryfall.io/normal/front/2/8/28180667-cc1e-4f64-9a69-00425ef85ba0.jpg?1721428800"
        ruling("2020-11-10", "If your commander is a card that has no colors in its color identity, Arcane Signet's ability produces no mana. It doesn't produce {C}.")
        ruling("2020-11-10", "If you have two commanders, the ability adds one mana of any color in their combined color identities.")
        ruling("2020-11-10", "If you don't have a commander, Arcane Signet's ability produces no mana.")
    }
}
