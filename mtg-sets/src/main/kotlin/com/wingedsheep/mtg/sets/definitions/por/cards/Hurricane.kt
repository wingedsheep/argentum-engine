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
 * Hurricane
 * {X}{G}
 * Sorcery
 * Hurricane deals X damage to each creature with flying and each player.
 */
val Hurricane = card("Hurricane") {
    manaCost = "{X}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    spell {
        effect = CompositeEffect(
        listOf(
            ForEachInGroupEffect(GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING)), DealDamageEffect(DynamicAmount.XValue, EffectTarget.Self)),
            ForEachPlayerEffect(Player.Each, listOf(DealDamageEffect(DynamicAmount.XValue, EffectTarget.Controller)))
        )
    )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "170"
        artist = "Andrew Robinson"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b97904e-80ba-4d65-808a-a528200430f8.jpg"
    }
}
