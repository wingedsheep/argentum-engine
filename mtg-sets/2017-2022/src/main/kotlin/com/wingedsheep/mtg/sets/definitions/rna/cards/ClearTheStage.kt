package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Clear the Stage
 * {4}{B}
 * Instant
 * Target creature gets -3/-3 until end of turn. If you control a creature with power 4 or greater,
 * you may return up to one target creature card from your graveyard to your hand.
 *
 * Both targets are chosen on casting; the power check happens on resolution, after the -3/-3 has
 * applied.
 */
val ClearTheStage = card("Clear the Stage") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Target creature gets -3/-3 until end of turn. If you control a creature with power 4 " +
        "or greater, you may return up to one target creature card from your graveyard to your hand."

    spell {
        val victim = target(TargetFilter.Creature)
        val creatureCard = target(TargetFilter.CreatureInYourGraveyard, optional = true)
        effect = Effects.ModifyStats(-3, -3, victim) then Effects.If(
            condition = Exists(Player.You, Zone.BATTLEFIELD, GameObjectFilter.Creature.powerAtLeast(4)),
            then = Effects.May(Effects.Move(creatureCard, Zone.HAND))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "68"
        artist = "Lucas Graciano"
        flavorText = "\"Make way! It's time for the final act!\""
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0ac65975-6ff5-4863-b7b7-d7dbea213b50.jpg?1783933695"
    }
}
