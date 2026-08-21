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
 * Tanglepool Bridge — Modern Horizons 2 #257
 * (no mana cost) · Artifact Land
 *
 * This land enters tapped.
 * Indestructible
 * {T}: Add {G} or {U}.
 *
 * One of Modern Horizons 2's ten "Bridge" artifact lands. See [DarkmossBridge] for the cycle's
 * modelling notes: the blank `manaCost`, why the printed "or" becomes two separate mana abilities
 * rather than one resolution-time choice, and why indestructible is the bare [Keyword].
 *
 * `colorIdentity` is "GU" — read off the mana symbols in the rules text (CR 903.4), because a land
 * has no mana cost to take it from.
 */
val TanglepoolBridge = card("Tanglepool Bridge") {
    manaCost = ""
    colorIdentity = "GU"
    typeLine = "Artifact Land"
    oracleText = "This land enters tapped.\n" +
        "Indestructible\n" +
        "{T}: Add {G} or {U}."

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
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "257"
        artist = "Randy Gallegos"
        flavorText = "The path to change is forged in insight."
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57d2b895-8921-4615-a674-fb85eed5ea3f.jpg?1783926793"
    }
}
