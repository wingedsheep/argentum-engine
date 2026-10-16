package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Night Clubber
 * {1}{B}{B}
 * Creature — Human Warrior
 * 2/2
 * When this creature enters, creatures your opponents control get -1/-1 until end of turn.
 * Blitz {2}{B} (If you cast this spell for its blitz cost, it gains haste and "When this creature dies, draw a card." Sacrifice it at the beginning of the next end step.)
 */
val NightClubber = card("Night Clubber") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warrior"
    oracleText = "When this creature enters, creatures your opponents control get -1/-1 until end of turn.\nBlitz {2}{B} (If you cast this spell for its blitz cost, it gains haste and \"When this creature dies, draw a card.\" Sacrifice it at the beginning of the next end step.)"
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesOpponentsControl,
            Effects.ModifyStats(-1, -1, EffectTarget.IterationEntity)
        )
    }

    keywordAbility(KeywordAbility.blitz("{2}{B}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "89"
        artist = "Joshua Raphael"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59fd3f8a-a13b-4a0a-bb27-b9246949ea7b.jpg?1783923126"
    }
}
