package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DealDamageEffect
import com.wingedsheep.sdk.scripting.DealDamageToPlayersEffect
import com.wingedsheep.sdk.scripting.DynamicAmount
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.GroupFilter

/**
 * Earthquake
 * {X}{R}
 * Sorcery
 * Earthquake deals X damage to each creature without flying and each player.
 */
val Earthquake = card("Earthquake") {
    manaCost = "{X}{R}"
    typeLine = "Sorcery"

    spell {
        effect = ForEachInGroupEffect(GroupFilter.AllCreatures.withoutKeyword(Keyword.FLYING), DealDamageEffect(DynamicAmount.XValue, EffectTarget.Self)) then
            DealDamageToPlayersEffect(DynamicAmount.XValue)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "124"
        artist = "Dan Frazier"
        flavorText = "The ground itself becomes a weapon."
        imageUri = "https://cards.scryfall.io/normal/front/2/7/272f65a3-3c0c-417d-b5b6-276a643d643e.jpg"
    }
}
