package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Moodmark Painter
 * {2}{B}{B}
 * Creature — Human Shaman
 * 2/3
 * Undergrowth — When this creature enters, target creature gains menace and gets +X/+0 until end of turn,
 * where X is the number of creature cards in your graveyard.
 */
val MoodmarkPainter = card("Moodmark Painter") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Shaman"
    oracleText = "Undergrowth — When this creature enters, target creature gains menace and gets +X/+0 until end of turn, " +
        "where X is the number of creature cards in your graveyard. (It can't be blocked except by two or more creatures.)"
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.GrantKeyword(Keyword.MENACE, creature) then
            Effects.ModifyStats(
                DynamicAmounts.creatureCardsInYourGraveyard(),
                DynamicAmounts.fixed(0),
                creature
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Scott Murphy"
        imageUri = "https://cards.scryfall.io/normal/front/8/2/82903e5f-c10f-4767-a2c0-6f8cc7280fa0.jpg?1783934172"
        ruling("2018-10-05", "The value of X is determined only as the undergrowth ability resolves. If the number of creature cards in your graveyard changes later in the turn, the target creature is unaffected.")
    }
}
