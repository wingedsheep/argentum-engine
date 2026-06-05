// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount


/**
 * Earthquake
 * {X}{R}
 * Sorcery
 * Earthquake deals X damage to each creature without flying and each player.
 */
val Earthquake = card("Earthquake") {
    manaCost = "{X}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    spell {
        effect = CompositeEffect(
        listOf(
            ForEachInGroupEffect(GroupFilter(GameObjectFilter.Creature.withoutKeyword(Keyword.FLYING)), DealDamageEffect(DynamicAmount.XValue, EffectTarget.Self)),
            ForEachPlayerEffect(Player.Each, listOf(DealDamageEffect(DynamicAmount.XValue, EffectTarget.Controller)))
        )
    )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Adrian Smith"
        imageUri = "https://cards.scryfall.io/normal/front/2/7/272f65a3-3c0c-417d-b5b6-276a643d643e.jpg"
    }
}
