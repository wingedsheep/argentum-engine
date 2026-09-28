package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Burning Sun's Fury
 * {1}{R}
 * Instant
 * Convoke
 * Up to two target creatures each get +2/+0 and gain haste until end of turn.
 */
val BurningSunsFury = card("Burning Sun's Fury") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText =
        "Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "Up to two target creatures each get +2/+0 and gain haste until end of turn."

    keywords(Keyword.CONVOKE)

    spell {
        target = TargetObject(filter = TargetFilter(GameObjectFilter.Creature), count = 2, minCount = 0)
        effect = Effects.ForEachTarget(
            Effects.ModifyStats(2, 0, EffectTarget.ContextTarget(0)),
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.ContextTarget(0)),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "133"
        artist = "Slawomir Maniak"
        flavorText = "Ixalan greeted Phyrexia with a defiant roar."
        imageUri = "https://cards.scryfall.io/normal/front/2/7/2731e181-f59c-446c-bb86-8f93bfbb10e9.jpg?1783916997"
    }
}
