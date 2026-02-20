package com.wingedsheep.mtg.sets.definitions.portal.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageToPlayersEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Hurricane
 * {X}{G}
 * Sorcery
 * Hurricane deals X damage to each creature with flying and each player.
 */
val Hurricane = card("Hurricane") {
    manaCost = "{X}{G}"
    typeLine = "Sorcery"

    spell {
        effect = ForEachInGroupEffect(GroupFilter.AllCreatures.withKeyword(Keyword.FLYING), DealDamageEffect(DynamicAmount.XValue, EffectTarget.Self)) then
            DealDamageToPlayersEffect(DynamicAmount.XValue)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "170"
        artist = "Cornelius Brudi"
        flavorText = "The wind knows no master but the sky."
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b97904e-80ba-4d65-808a-a528200430f8.jpg"
    }
}
