package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.triggers.OnEnterBattlefield
import com.wingedsheep.sdk.scripting.effects.TapUntapEffect

/**
 * Thundermare
 * {5}{R}
 * Creature — Elemental Horse
 * 5/5
 * Haste
 * When Thundermare enters the battlefield, tap all other creatures.
 */
val Thundermare = card("Thundermare") {
    manaCost = "{5}{R}"
    typeLine = "Creature — Elemental Horse"
    power = 5
    toughness = 5

    keywords(Keyword.HASTE)

    triggeredAbility {
        trigger = OnEnterBattlefield()
        effect = ForEachInGroupEffect(GroupFilter.AllOtherCreatures, TapUntapEffect(EffectTarget.Self, tap = true))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "152"
        artist = "Una Fricker"
        flavorText = "Thunder and lightning herald the coming storm."
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59a9f3f5-c80f-47a4-bf84-b7262437017f.jpg"
    }
}
