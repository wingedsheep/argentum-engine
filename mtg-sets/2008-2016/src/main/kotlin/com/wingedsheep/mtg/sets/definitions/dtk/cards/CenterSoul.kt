package com.wingedsheep.mtg.sets.definitions.dtk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Center Soul
 * {1}{W}
 * Instant
 * Target creature you control gains protection from the color of your choice until end of turn.
 * Rebound
 *
 * Gods Willing's shape (colour chosen on resolution) plus [Keyword.REBOUND], which the spell
 * resolver reads to exile a hand-cast copy and offer the free recast on your next upkeep.
 */
val CenterSoul = card("Center Soul") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Target creature you control gains protection from the color of your choice until end of turn.\n" +
        "Rebound (If you cast this spell from your hand, exile it as it resolves. At the beginning of your " +
        "next upkeep, you may cast this card from exile without paying its mana cost.)"

    keywords(Keyword.REBOUND)

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.ChooseColorThen(Effects.GrantProtectionFromChosenColor(creature))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "8"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bfca3763-9823-4fa4-968d-c701434b3d28.jpg"
    }
}
