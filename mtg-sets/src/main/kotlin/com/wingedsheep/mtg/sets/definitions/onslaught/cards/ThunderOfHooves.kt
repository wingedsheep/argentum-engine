package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageToPlayersEffect
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Thunder of Hooves
 * {3}{R}
 * Sorcery
 * Thunder of Hooves deals X damage to each creature without flying and each player,
 * where X is the number of Beasts on the battlefield.
 */
val ThunderOfHooves = card("Thunder of Hooves") {
    manaCost = "{3}{R}"
    typeLine = "Sorcery"
    oracleText = "Thunder of Hooves deals X damage to each creature without flying and each player, where X is the number of Beasts on the battlefield."

    val beastCount = DynamicAmount.AggregateBattlefield(Player.Each, GameObjectFilter.Creature.withSubtype("Beast"))

    spell {
        effect = ForEachInGroupEffect(GroupFilter.AllCreatures.withoutKeyword(Keyword.FLYING), DealDamageEffect(beastCount, EffectTarget.Self)) then
            DealDamageToPlayersEffect(beastCount)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "242"
        artist = "Jim Nelson"
        flavorText = ""
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e4f796a-6831-4d83-824d-88fd2148b4c1.jpg?1562931476"
    }
}
