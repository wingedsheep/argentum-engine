// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget


/**
 * Thundermare
 * {5}{R}
 * Creature — Elemental Horse
 * 5/5
 * Haste (This creature can attack and {T} as soon as it comes under your control.)
 * When this creature enters, tap all other creatures.
 */
val Thundermare = card("Thundermare") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental Horse"
    power = 5
    toughness = 5
    keywords(Keyword.HASTE)
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ForEachInGroupEffect(GroupFilter(GameObjectFilter.Creature, excludeSelf = true), Effects.Tap(EffectTarget.Self))
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "152"
        artist = "Bob Eggleton"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59a9f3f5-c80f-47a4-bf84-b7262437017f.jpg"
    }
}
