package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Confront the Unknown — Shadows over Innistrad #198
 * {G} · Instant
 *
 * Investigate, then target creature gets +1/+1 until end of turn for each Clue you control.
 *
 * The investigate runs first, so the Clue it creates is counted: the creature always gets at least
 * +1/+1. The count is read once as the pump resolves and stays locked for the turn, even if a Clue is
 * sacrificed later. "Clue" is the artifact subtype, so any Clue artifact you control counts.
 */
val ConfrontTheUnknown = card("Confront the Unknown") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Investigate, then target creature gets +1/+1 until end of turn for each Clue you control. " +
        "(Create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    spell {
        val creature = target(TargetFilter.Creature)
        val clues = DynamicAmounts.count(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Artifact.withSubtype("Clue"))
        effect = Effects.Investigate() then Effects.ModifyStats(clues, clues, creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "198"
        artist = "Kev Walker"
        flavorText = "\"Never flinch from the truth.\"\n—Thalia, Guardian of Thraben"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c59557c3-aba2-4f80-8f37-accb7b991cb9.jpg?1783937733"
        ruling(
            "2016-04-08",
            "No player may take any action in between the two steps of Confront the Unknown, so the " +
                "target creature will get at least +1/+1.",
        )
        ruling(
            "2016-04-08",
            "If an effect refers to a Clue, it means any Clue artifact, not just a Clue artifact token.",
        )
        ruling(
            "2016-04-08",
            "You can't cast a spell without choosing legal targets. If all of those targets become " +
                "illegal, the spell doesn't resolve and you won't investigate.",
        )
    }
}
