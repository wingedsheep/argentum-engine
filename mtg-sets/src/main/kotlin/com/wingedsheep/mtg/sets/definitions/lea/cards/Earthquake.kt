package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

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
        effect = ForEachInGroupEffect(GroupFilter.AllCreatures.withoutKeyword(Keyword.FLYING), DealDamageEffect(DynamicAmount.XValue, EffectTarget.Self)) then
            Effects.DealDamage(DynamicAmount.XValue, EffectTarget.PlayerRef(Player.Each))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "146"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e68ac362-6cdc-48a6-bdd3-4f8ea32add64.jpg?1559591701"
    }
}
