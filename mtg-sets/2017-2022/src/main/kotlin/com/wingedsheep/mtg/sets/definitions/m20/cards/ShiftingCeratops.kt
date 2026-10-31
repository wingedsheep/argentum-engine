package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.ProtectionScope
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Shifting Ceratops — Core Set 2020 #194
 * {2}{G}{G} · Creature — Dinosaur · 5/4
 *
 * "Your choice of reach, trample, or haste" is chosen on resolution, so the activated ability is a
 * resolution-time [Effects.ChooseAction] over three keyword grants on itself.
 */
val ShiftingCeratops = card("Shifting Ceratops") {
    manaCost = "{2}{G}{G}"
    typeLine = "Creature — Dinosaur"
    power = 5
    toughness = 4
    oracleText = "This spell can't be countered.\n" +
        "Protection from blue (This creature can't be blocked, targeted, dealt damage, enchanted, or equipped by anything blue.)\n" +
        "{G}: This creature gains your choice of reach, trample, or haste until end of turn."

    cantBeCountered = true
    keywordAbility(KeywordAbility.Protection(ProtectionScope.Color(Color.BLUE)))

    activatedAbility {
        cost = Costs.Mana("{G}")
        effect = Effects.ChooseAction(listOf(
            EffectChoice(
                label = "It gains reach until end of turn",
                effect = Effects.GrantKeyword(Keyword.REACH, EffectTarget.Self),
            ),
            EffectChoice(
                label = "It gains trample until end of turn",
                effect = Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.Self),
            ),
            EffectChoice(
                label = "It gains haste until end of turn",
                effect = Effects.GrantKeyword(Keyword.HASTE, EffectTarget.Self),
            )
        ))
        description = "{G}: This creature gains your choice of reach, trample, or haste until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "194"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e318600-21af-4003-bcfa-be926cd24c5c.jpg?1783932957"
        ruling("2019-07-12", "A spell or ability that counters spells can still target Shifting Ceratops. When that spell or ability resolves, Shifting Ceratops won't be countered, but any additional effects of that spell or ability will still happen.")
        ruling("2019-07-12", "Multiple instances of reach, trample, and/or haste on the same creature are redundant.")
    }
}
