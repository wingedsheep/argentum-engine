// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

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


/**
 * Fire Tempest
 * {5}{R}{R}
 * Sorcery
 * Fire Tempest deals 6 damage to each creature and each player.
 */
val FireTempest = card("Fire Tempest") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    spell {
        effect = CompositeEffect(
        listOf(
            ForEachInGroupEffect(GroupFilter(GameObjectFilter.Creature), DealDamageEffect(6, EffectTarget.Self)),
            ForEachPlayerEffect(Player.Each, listOf(DealDamageEffect(6, EffectTarget.Controller)))
        )
    )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "128"
        artist = "Mike Dringenberg"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92334ebe-3d7a-46de-8b91-931e5d56a5a5.jpg"
    }
}
