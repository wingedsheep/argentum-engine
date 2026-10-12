package com.wingedsheep.mtg.sets.definitions.wwk.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hammer of Ruin — Worldwake #124
 * {2} · Artifact — Equipment
 *
 * Equipped creature gets +2/+0.
 * Whenever equipped creature deals combat damage to a player, you may destroy target Equipment
 * that player controls.
 * Equip {2}
 *
 * "That player" is the damaged player: the ATTACHED damage trigger carries the same
 * `TriggerContext.fromEvent` as a self-bound one, so `controlledByTriggeringPlayer()` scopes the
 * target exactly as on Rustmouth Ogre. [Effects.May] around the destroy is the "you may"; the
 * engine takes its answer at the target prompt (a target is yes, Decline skips the trigger).
 */
val HammerOfRuin = card("Hammer of Ruin") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+0.\n" +
        "Whenever equipped creature deals combat damage to a player, you may destroy target Equipment " +
        "that player controls.\n" +
        "Equip {2}"

    staticAbility {
        ability = ModifyStats(2, 0, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.dealsCombatDamage(Recipient.AnyPlayer)
        val t = target(
            TargetFilter(GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT).controlledByTriggeringPlayer())
        )
        effect = Effects.May(Effects.Destroy(t))
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "124"
        artist = "Vincent Proce"
        flavorText = "By hammer forged, and by hammer undone."
        imageUri = "https://cards.scryfall.io/normal/front/3/0/308c6bc5-701d-4ae8-bb52-c2e2c6956946.jpg?1783942040"
    }
}
