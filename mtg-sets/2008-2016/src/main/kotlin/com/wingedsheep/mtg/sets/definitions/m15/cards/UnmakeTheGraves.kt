package com.wingedsheep.mtg.sets.definitions.m15.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Unmake the Graves
 * {4}{B}
 * Instant
 * Convoke
 * Return up to two target creature cards from your graveyard to your hand.
 */
val UnmakeTheGraves = card("Unmake the Graves") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText =
        "Convoke (Your creatures can help cast this spell. Each creature you tap while casting this spell pays for {1} or one mana of that creature's color.)\n" +
        "Return up to two target creature cards from your graveyard to your hand."

    keywords(Keyword.CONVOKE)

    spell {
        target = TargetObject(
            count = 2,
            optional = true,
            filter = TargetFilter.CreatureInYourGraveyard
        )
        effect = Effects.ForEachTarget(
            Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "120"
        artist = "Aaron Miller"
        flavorText = "\"I'm raising an army. Any volunteers?\""
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6fd00bb-b247-45b7-9cb2-0d65b92c0360.jpg?1783939179"
    }
}
