package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

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
        effect = Effects.ForEachInGroup(GroupFilter.AllCreatures.withKeyword(Keyword.FLYING), DealDamageEffect(DynamicAmount.XValue, EffectTarget.Self)) then
            Effects.DealDamage(DynamicAmount.XValue, EffectTarget.PlayerRef(Player.Each))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "200"
        artist = "Dameon Willich"
        imageUri = "https://cards.scryfall.io/normal/front/5/2/52f5a19f-16e4-4d35-89e1-969ac8202f88.jpg?1559591426"
    }
}
