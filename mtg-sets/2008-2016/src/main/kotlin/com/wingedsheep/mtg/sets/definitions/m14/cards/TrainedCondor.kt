package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Trained Condor
 * {2}{U}
 * Creature — Bird
 * 2/1
 * Flying
 * Whenever this creature attacks, another target creature you control gains flying until end of turn.
 */
val TrainedCondor = card("Trained Condor") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Bird"
    power = 2
    toughness = 1
    oracleText = "Flying\nWhenever this creature attacks, another target creature you control gains flying until end of turn."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        val creature = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.GrantKeyword(Keyword.FLYING, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "76"
        artist = "Alex Horley-Orlandelli"
        flavorText = "\"They're smart enough to understand that war means lots of carrion.\"\n—Griv, condor rider"
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6e1eaa5a-3f9d-4166-b418-fd82fff86c73.jpg?1783939929"
    }
}
