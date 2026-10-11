package com.wingedsheep.mtg.sets.definitions.uds.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Sigil of Sleep
 * {U}
 * Enchantment — Aura
 * Enchant creature
 * Whenever enchanted creature deals damage to a player, return target creature that player
 * controls to its owner's hand.
 *
 * "That player" is the damaged player: the ATTACHED damage trigger carries the damaged player as
 * the triggering player (as on Hammer of Ruin), so `controlledByTriggeringPlayer()` scopes the
 * target. Any damage counts, combat or not.
 */
val SigilOfSleep = card("Sigil of Sleep") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Whenever enchanted creature deals damage to a player, return target creature that player " +
        "controls to its owner's hand."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.dealsDamage(Recipient.AnyPlayer)
        val t = target(TargetFilter(GameObjectFilter.Creature.controlledByTriggeringPlayer()))
        effect = Effects.ReturnToHand(t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "46"
        artist = "Greg Hildebrandt & Tim Hildebrandt"
        flavorText = "Arrows are only one way to remove an enemy."
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a31f3c70-e2c3-479e-8d22-2fd1429e9857.jpg?1783946076"
    }
}
