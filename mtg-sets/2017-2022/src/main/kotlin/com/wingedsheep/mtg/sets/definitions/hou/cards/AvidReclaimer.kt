package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Avid Reclaimer — Hour of Devastation #201 (canonical printing)
 * {2}{G} · Creature — Human Druid · 2/2
 *
 * {T}: Add {G} or {U}. If you control a Nissa planeswalker, you gain 2 life.
 *
 * One mana ability per colour, each adding its mana and then gaining 2 life when a Nissa
 * planeswalker is on your side — checked as the mana ability resolves (it never uses the stack).
 */
private val controlsNissa = Conditions.YouControl(GameObjectFilter.Planeswalker.withSubtype("Nissa"))

val AvidReclaimer = card("Avid Reclaimer") {
    manaCost = "{2}{G}"
    colorIdentity = "GU"
    typeLine = "Creature — Human Druid"
    power = 2
    toughness = 2
    oracleText = "{T}: Add {G} or {U}. If you control a Nissa planeswalker, you gain 2 life."

    for (color in listOf(Color.GREEN, Color.BLUE)) {
        activatedAbility {
            cost = Costs.Tap
            effect = Effects.AddMana(color) then Effects.If(controlsNissa, Effects.GainLife(2))
            manaAbility = true
            timing = TimingRule.ManaAbility
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "201"
        artist = "Josu Hernaiz"
        flavorText = "\"All can't be lost. Nissa has shown me that.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/4/0468e488-94ce-4ae3-abe4-7782673a7e62.jpg?1783935987"
        ruling("2017-07-14", "You can activate Avid Reclaimer's ability even if you don't have anything to spend that mana on. You'll still gain 2 life if you control a Nissa planeswalker.")
        ruling("2017-07-14", "Avid Reclaimer's ability is a mana ability. It doesn't use the stack and can't be responded to. You'll immediately add mana and gain 2 life, if applicable.")
    }
}
