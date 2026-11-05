package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vineshaper Mystic — Ixalan #214 (canonical printing)
 * {2}{G} · Creature — Merfolk Shaman · 1/3
 *
 * When this creature enters, put a +1/+1 counter on each of up to two target Merfolk you control.
 *
 * One target requirement taking up to two Merfolk you control (itself included), fanned out with
 * [Effects.ForEachTarget] — the Gavony Silversmith idiom.
 */
val VineshaperMystic = card("Vineshaper Mystic") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Merfolk Shaman"
    power = 1
    toughness = 3
    oracleText = "When this creature enters, put a +1/+1 counter on each of up to two target Merfolk you control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        targets(
            TargetFilter(GameObjectFilter.Permanent.withSubtype(Subtype.MERFOLK).youControl()),
            count = 2,
            optional = true,
        )
        effect = Effects.ForEachTarget(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0))
        )
        description = "When this creature enters, put a +1/+1 counter on each of up to two target Merfolk you control."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "214"
        artist = "Kieran Yanner"
        flavorText = "She has never touched a loom, yet the tapestries she weaves are vast and beautiful."
        imageUri = "https://cards.scryfall.io/normal/front/c/4/c4a27502-f6f4-4c12-af0e-eab00d942ada.jpg?1783935716"
    }
}
