package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Eriette's Whisper
 * {3}{B}
 * Sorcery
 * Target opponent discards two cards. Create a Wicked Role token attached to up to one target
 * creature you control. (If you control another Role on it, put that one into the graveyard.
 * Enchanted creature gets +1/+1. When this token is put into a graveyard, each opponent loses 1 life.)
 *
 * Two independent targets: the opponent who discards, and the optional ("up to one") creature that
 * gains the Wicked Role. Role replacement (an existing Role you control on that creature is put into
 * the graveyard first) is handled by the CreateRoleToken executor; the Wicked Role token itself
 * carries the +1/+1 buff and the graveyard-drain trigger.
 */
val EriettesWhisper = card("Eriette's Whisper") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Target opponent discards two cards. Create a Wicked Role token attached to up to " +
        "one target creature you control. (If you control another Role on it, put that one into the " +
        "graveyard. Enchanted creature gets +1/+1. When this token is put into a graveyard, each " +
        "opponent loses 1 life.)"

    spell {
        val opponent = target("opponent", Targets.Opponent)
        val creature = target(
            "creature",
            TargetCreature(optional = true, filter = TargetFilter.CreatureYouControl)
        )
        effect = Effects.Composite(
            Effects.Discard(2, opponent),
            Effects.CreateRoleToken("Wicked Role", creature)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "88"
        artist = "Quintin Gleim"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfed2acf-9ac8-447b-94f5-7db3a713c991.jpg?1783915109"
    }
}
