package com.wingedsheep.mtg.sets.definitions.jou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.SacrificeSelfEffect
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Flamespeaker's Will
 * {R}
 * Enchantment — Aura
 *
 * Enchant creature you control
 * Enchanted creature gets +1/+1.
 * Whenever enchanted creature deals combat damage to a player, you may sacrifice this Aura. If you
 * do, destroy target artifact.
 *
 * The target artifact is chosen as the trigger goes on the stack; on resolution
 * [Effects.MayPay] over [SacrificeSelfEffect] asks whether to sacrifice the Aura (only offered while
 * it is still on the battlefield, per the ruling) and destroys the target only if it was sacrificed.
 */
val FlamespeakersWill = card("Flamespeaker's Will") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature you control\n" +
        "Enchanted creature gets +1/+1.\n" +
        "Whenever enchanted creature deals combat damage to a player, you may sacrifice this Aura. " +
        "If you do, destroy target artifact."

    auraTarget = TargetObject(filter = TargetFilter.CreatureYouControl)

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    triggeredAbility {
        trigger = Triggers.attached.dealsCombatDamage(Recipient.AnyPlayer)
        val t = target(TargetFilter.Artifact)
        effect = Effects.MayPay(
            cost = SacrificeSelfEffect,
            then = Effects.Destroy(t)
        )
        description = "Whenever enchanted creature deals combat damage to a player, you may " +
            "sacrifice this Aura. If you do, destroy target artifact."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "95"
        artist = "Daniel Ljunggren"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/32b5e3bb-f9e8-4a74-9798-41d8f8515e12.jpg?1783939425"

        ruling("2014-04-26", "You decide whether to sacrifice Flamespeaker's Will as its triggered ability resolves. If it's not on the battlefield at that time, you can't sacrifice it and destroy the target artifact.")
        ruling("2014-04-26", "If another player gains control of the enchanted creature, Flamespeaker's Will will be put into its owner's graveyard.")
    }
}
