package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * End-Raze Forerunners
 * {5}{G}{G}{G}
 * Creature — Boar
 * 7/7
 *
 * Vigilance, trample, haste
 * When this creature enters, other creatures you control get +2/+2 and gain vigilance and trample
 * until end of turn.
 */
val EndRazeForerunners = card("End-Raze Forerunners") {
    manaCost = "{5}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Boar"
    oracleText = "Vigilance, trample, haste\n" +
        "When this creature enters, other creatures you control get +2/+2 and gain vigilance and trample until end of turn."
    power = 7
    toughness = 7

    keywords(Keyword.VIGILANCE, Keyword.TRAMPLE, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true),
            Effects.ModifyStats(2, 2, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.IterationEntity) then
                Effects.GrantKeyword(Keyword.TRAMPLE, EffectTarget.IterationEntity)
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Mathias Kollros"
        flavorText = "\"Smash this city to pieces.\"\n—Domri Rade"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a50d79fe-6d37-42f3-b7b0-0c3018282fa2.jpg"
    }
}
