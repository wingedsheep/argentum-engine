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
 * Thornglint Bridge — Modern Horizons 2 #258
 * (no mana cost) · Artifact Land
 *
 * This land enters tapped.
 * Indestructible
 * {T}: Add {G} or {W}.
 *
 * One of Modern Horizons 2's ten "Bridge" artifact lands. See [DarkmossBridge] for the cycle's
 * modelling notes: the blank `manaCost`, why the printed "or" becomes two separate mana abilities
 * rather than one resolution-time choice, and why indestructible is the bare [Keyword].
 *
 * `colorIdentity` is "GW" — read off the mana symbols in the rules text (CR 903.4), because a land
 * has no mana cost to take it from.
 */
val ThornglintBridge = card("Thornglint Bridge") {
    manaCost = ""
    colorIdentity = "GW"
    typeLine = "Artifact Land"
    oracleText = "This land enters tapped.\n" +
        "Indestructible\n" +
        "{T}: Add {G} or {W}."

    replacementEffect(EntersTapped())
    keywords(Keyword.INDESTRUCTIBLE)

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.GREEN)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "258"
        artist = "Randy Gallegos"
        flavorText = "The path to growth is forged in balance."
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d483643-6a11-47f7-a0aa-190e0179525f.jpg?1783926792"
    }
}
