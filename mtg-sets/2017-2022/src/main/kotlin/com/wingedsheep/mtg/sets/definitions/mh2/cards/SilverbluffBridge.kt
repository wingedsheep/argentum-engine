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
 * Silverbluff Bridge — Modern Horizons 2 #255
 * (no mana cost) · Artifact Land
 *
 * This land enters tapped.
 * Indestructible
 * {T}: Add {U} or {R}.
 *
 * One of Modern Horizons 2's ten "Bridge" artifact lands. See [DarkmossBridge] for the cycle's
 * modelling notes: the blank `manaCost`, why the printed "or" becomes two separate mana abilities
 * rather than one resolution-time choice, and why indestructible is the bare [Keyword].
 *
 * `colorIdentity` is "RU" — read off the mana symbols in the rules text (CR 903.4), because a land
 * has no mana cost to take it from.
 */
val SilverbluffBridge = card("Silverbluff Bridge") {
    manaCost = ""
    colorIdentity = "RU"
    typeLine = "Artifact Land"
    oracleText = "This land enters tapped.\n" +
        "Indestructible\n" +
        "{T}: Add {U} or {R}."

    replacementEffect(EntersTapped())
    keywords(Keyword.INDESTRUCTIBLE)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.RED)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "255"
        artist = "Joseph Meehan"
        flavorText = "The path to genius is forged in creativity."
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d80dc025-c2c4-48c2-8354-7d9ddb430eb9.jpg?1783926793"
    }
}
