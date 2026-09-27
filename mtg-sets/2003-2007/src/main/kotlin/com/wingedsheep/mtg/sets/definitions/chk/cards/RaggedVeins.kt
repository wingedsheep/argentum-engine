package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Ragged Veins — Champions of Kamigawa #139
 * {1}{B} · Enchantment — Aura
 *
 * Flash
 * Enchant creature
 * Whenever enchanted creature is dealt damage, its controller loses that much life.
 *
 * The trigger stays on the Aura (`Triggers.attached.isDealtDamage()`), so the Aura's controller
 * controls it, as printed. The attached damage trigger's context names the damaged creature as the
 * triggering entity and stamps its controller *as the damage was dealt* (last-known information)
 * as the triggering player — so [Player.TriggeringPlayer] is "its controller" even when the damage
 * was lethal and the creature is already in the graveyard when the trigger resolves. "That much"
 * is the damage amount off the trigger context; each damage event triggers separately.
 */
val RaggedVeins = card("Ragged Veins") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\nEnchant creature\n" +
        "Whenever enchanted creature is dealt damage, its controller loses that much life."

    keywords(Keyword.FLASH)

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.isDealtDamage()
        effect = Effects.LoseLife(
            DynamicAmounts.triggerDamageAmount(),
            EffectTarget.PlayerRef(Player.TriggeringPlayer),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "139"
        artist = "Chippy"
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7f3312f-71d0-4dfd-ba39-c2a2ff8d5bd0.jpg?1783944308"
    }
}
