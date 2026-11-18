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
 * Mortal Obstinacy
 * {W}
 * Enchantment — Aura
 *
 * Enchant creature you control
 * Enchanted creature gets +1/+1.
 * Whenever enchanted creature deals combat damage to a player, you may sacrifice this Aura. If you
 * do, destroy target enchantment.
 *
 * The target enchantment is chosen as the trigger goes on the stack; on resolution
 * [Effects.MayPay] over [SacrificeSelfEffect] asks whether to sacrifice the Aura (only offered while
 * it is still on the battlefield, per the ruling) and destroys the target only if it was sacrificed.
 */
val MortalObstinacy = card("Mortal Obstinacy") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature you control\n" +
        "Enchanted creature gets +1/+1.\n" +
        "Whenever enchanted creature deals combat damage to a player, you may sacrifice this Aura. " +
        "If you do, destroy target enchantment."

    auraTarget = TargetObject(filter = TargetFilter.CreatureYouControl)

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    triggeredAbility {
        trigger = Triggers.attached.dealsCombatDamage(Recipient.AnyPlayer)
        val t = target(TargetFilter.Enchantment)
        effect = Effects.MayPay(
            cost = SacrificeSelfEffect,
            then = Effects.Destroy(t)
        )
        description = "Whenever enchanted creature deals combat damage to a player, you may " +
            "sacrifice this Aura. If you do, destroy target enchantment."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "17"
        artist = "Slawomir Maniak"
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67652df1-f2cc-44cd-ac66-9722d4cd86e9.jpg?1783939456"

        ruling("2014-04-26", "You decide whether to sacrifice Mortal Obstinacy as its triggered ability resolves. If it's not on the battlefield at that time, you can't sacrifice it and destroy the target enchantment.")
        ruling("2014-04-26", "If another player gains control of the enchanted creature, Mortal Obstinacy will be put into its owner's graveyard.")
    }
}
