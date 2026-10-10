package com.wingedsheep.mtg.sets.definitions.conflux.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Noble Hierarch
 * {G}
 * Creature — Human Druid
 * 0/1
 * Exalted
 * {T}: Add {G}, {W}, or {U}.
 *
 * Exalted is the printed keyword; the engine derives its trigger from the projected keyword. The
 * printed "or" is three separate mana abilities sharing [Costs.Tap], the same shape as Druid of
 * the Anima.
 */
val NobleHierarch = card("Noble Hierarch") {
    manaCost = "{G}"
    colorIdentity = "GWU"
    typeLine = "Creature — Human Druid"
    power = 0
    toughness = 1
    oracleText = "Exalted (Whenever a creature you control attacks alone, that creature gets +1/+1 " +
        "until end of turn.)\n{T}: Add {G}, {W}, or {U}."

    keywords(Keyword.EXALTED)

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
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.BLUE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "87"
        artist = "Mark Zug"
        flavorText = "She protects the sacred groves from blight, drought, and the Unbeholden."
        imageUri = "https://cards.scryfall.io/normal/front/6/a/6adfe928-1305-444d-b709-1e714544daaf.jpg?1783942473"
    }
}
