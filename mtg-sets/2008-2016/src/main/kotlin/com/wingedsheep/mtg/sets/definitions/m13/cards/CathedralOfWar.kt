package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Cathedral of War — Magic 2013 #221 (canonical printing)
 * Land
 *
 * This land enters tapped.
 * Exalted (Whenever a creature you control attacks alone, that creature gets +1/+1 until end of turn.)
 * {T}: Add {C}.
 *
 * Exalted is the printed keyword; the engine derives its trigger from the projected keyword on any
 * permanent, not just creatures (as on Angelic Benediction).
 */
val CathedralOfWar = card("Cathedral of War") {
    manaCost = ""
    typeLine = "Land"
    oracleText = "This land enters tapped.\nExalted (Whenever a creature you control attacks alone, " +
        "that creature gets +1/+1 until end of turn.)\n{T}: Add {C}."

    keywords(Keyword.EXALTED)

    replacementEffect(EntersTapped())

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "221"
        artist = "Kekai Kotaki"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd222c07-0b28-41cb-9237-ad7991ab078f.jpg?1783940455"
    }
}
