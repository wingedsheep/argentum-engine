package com.wingedsheep.mtg.sets.definitions.scg.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.dsl.Effects

/**
 * Reaping the Graves
 * {2}{B}
 * Instant
 * Return target creature card from your graveyard to your hand.
 * Storm (When you cast this spell, copy it for each spell cast before it this turn.
 * You may choose new targets for the copies.)
 */
val ReapingTheGraves = card("Reaping the Graves") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Return target creature card from your graveyard to your hand.\nStorm (When you cast this spell, copy it for each spell cast before it this turn. You may choose new targets for the copies.)"

    spell {
        target = TargetObject(filter = TargetFilter.CreatureInYourGraveyard)
        effect = Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND)
    }

    keywords(Keyword.STORM)

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "72"
        artist = "Ron Spencer"
        imageUri = "https://cards.scryfall.io/normal/front/7/6/760a66bd-2821-4710-8f02-3c30772dd884.jpg?1562530700"
    }
}
