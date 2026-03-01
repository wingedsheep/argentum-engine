package com.wingedsheep.mtg.sets.definitions.scourge.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.DestroyAllEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Decree of Pain
 * {6}{B}{B}
 * Sorcery
 * Destroy all creatures. They can't be regenerated. Draw a card for each creature destroyed this way.
 * Cycling {3}{B}{B}
 * When you cycle Decree of Pain, all creatures get -2/-2 until end of turn.
 */
val DecreeOfPain = card("Decree of Pain") {
    manaCost = "{6}{B}{B}"
    typeLine = "Sorcery"
    oracleText = "Destroy all creatures. They can't be regenerated. Draw a card for each creature destroyed this way.\nCycling {3}{B}{B}\nWhen you cycle Decree of Pain, all creatures get -2/-2 until end of turn."

    spell {
        effect = DestroyAllEffect(
            filter = GameObjectFilter.Creature,
            canRegenerate = false,
            storeDestroyedAs = "destroyed"
        ).then(
            DrawCardsEffect(DynamicAmount.VariableReference("destroyed_count"))
        )
    }

    keywordAbility(KeywordAbility.cycling("{3}{B}{B}"))

    triggeredAbility {
        trigger = Triggers.YouCycleThis
        effect = Effects.ModifyStatsForAll(-2, -2, GroupFilter.AllCreatures)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "64"
        artist = "Carl Critchlow"
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e1958a07-fc75-41cd-ac45-d92d49587754.jpg?1562536145"
        ruling("2022-12-08", "When you cycle this card, first the cycling ability goes on the stack, then the triggered ability goes on the stack on top of it. The triggered ability will resolve before you draw a card from the cycling ability.")
        ruling("2022-12-08", "The cycling ability and the triggered ability are separate. If the triggered ability doesn't resolve (because, for example, it has been countered, or all of its targets have become illegal), the cycling ability will still resolve, and you'll draw a card.")
    }
}
