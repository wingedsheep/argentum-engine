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
 * Goldmire Bridge — Modern Horizons 2 #247
 * (no mana cost) · Artifact Land
 *
 * This land enters tapped.
 * Indestructible
 * {T}: Add {W} or {B}.
 *
 * One of Modern Horizons 2's ten "Bridge" artifact lands. See [DarkmossBridge] for the cycle's
 * modelling notes: the blank `manaCost`, why the printed "or" becomes two separate mana abilities
 * rather than one resolution-time choice, and why indestructible is the bare [Keyword].
 *
 * `colorIdentity` is "BW" — read off the mana symbols in the rules text (CR 903.4), because a land
 * has no mana cost to take it from.
 */
val GoldmireBridge = card("Goldmire Bridge") {
    manaCost = ""
    colorIdentity = "BW"
    typeLine = "Artifact Land"
    oracleText = "This land enters tapped.\n" +
        "Indestructible\n" +
        "{T}: Add {W} or {B}."

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
        effect = Effects.AddMana(Color.BLACK)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "247"
        artist = "Aaron Miller"
        flavorText = "The path to glory is forged in ambition."
        imageUri = "https://cards.scryfall.io/normal/front/d/b/dbe2a1fa-196f-497f-a15f-0b3b04da9cbb.jpg?1783926796"
    }
}
