package com.wingedsheep.mtg.sets.definitions.m13.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Battleflight Eagle
 * {4}{W}
 * Creature — Bird
 * 2/2
 * Flying
 * When this creature enters, target creature gets +2/+2 and gains flying until end of turn.
 */
val BattleflightEagle = card("Battleflight Eagle") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Bird"
    oracleText = "Flying\n" +
        "When this creature enters, target creature gets +2/+2 and gains flying until end of turn."
    power = 2
    toughness = 2

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(TargetFilter.Creature)
        effect = Effects.ModifyStats(2, 2, t) then Effects.GrantKeyword(Keyword.FLYING, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "7"
        artist = "Kev Walker"
        flavorText = "Eagles make fine mounts, but they're too proud to carry anyone for long."
        imageUri = "https://cards.scryfall.io/normal/front/4/1/4182dbd5-8eae-4f4b-86aa-2bfc24481800.jpg"
    }
}
