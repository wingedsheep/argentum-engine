package com.wingedsheep.mtg.sets.definitions.mh2.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule


/**
 * Razortide Bridge — Modern Horizons 2 #252
 * (no mana cost) · Artifact Land
 *
 * This land enters tapped.
 * Indestructible
 * {T}: Add {W} or {U}.
 *
 * One of Modern Horizons 2's ten "Bridge" artifact lands. See [DarkmossBridge] for the cycle's
 * modelling notes: the blank `manaCost`, why the printed "or" becomes two separate mana abilities
 * rather than one resolution-time choice, and why indestructible is the bare [Keyword].
 *
 * `colorIdentity` is "UW" — read off the mana symbols in the rules text (CR 903.4), because a land
 * has no mana cost to take it from.
 */
val RazortideBridge = card("Razortide Bridge") {
    manaCost = ""
    colorIdentity = "UW"
    typeLine = "Artifact Land"
    oracleText = "This land enters tapped.\n" +
        "Indestructible\n" +
        "{T}: Add {W} or {U}."

    replacementEffect(EntersTapped())
    keywords(Keyword.INDESTRUCTIBLE)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "252"
        artist = "Rob Alexander"
        flavorText = "The path to unity is forged in understanding."
        imageUri = "https://cards.scryfall.io/normal/front/e/7/e7ea7395-430e-4036-92c9-17a850ec2371.jpg?1783926796"
    }
}
