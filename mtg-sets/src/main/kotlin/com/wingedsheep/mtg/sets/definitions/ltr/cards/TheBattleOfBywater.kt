package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * The Battle of Bywater
 * {1}{W}{W}
 * Sorcery
 *
 * Destroy all creatures with power 3 or greater. Then create a Food token for each creature
 * you control. (It's an artifact with "{2}, {T}, Sacrifice this token: You gain 3 life.")
 */
val TheBattleOfBywater = card("The Battle of Bywater") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Destroy all creatures with power 3 or greater. Then create a Food token for each creature you control. " +
        "(It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")"

    spell {
        effect = Effects.DestroyAll(GameObjectFilter.Creature.powerAtLeast(3)) then
            Effects.ForEachInGroup(
                filter = GroupFilter(GameObjectFilter.Creature.youControl()),
                effect = Effects.CreateFood()
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "2"
        artist = "Tomas Duchek"
        flavorText = "The Tooks marched in with Pippin at their head. Merry now had enough sturdy Hobbitry to deal with the ruffians."
        imageUri = "https://cards.scryfall.io/normal/front/9/b/9bae7a4d-9117-43c5-a048-80a0ddadc034.jpg?1708013075"
    }
}
