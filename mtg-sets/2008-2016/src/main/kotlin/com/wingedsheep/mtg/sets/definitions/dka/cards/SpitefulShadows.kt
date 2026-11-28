package com.wingedsheep.mtg.sets.definitions.dka.cards

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
 * Spiteful Shadows — Dark Ascension #75
 * {1}{B} · Enchantment — Aura
 *
 * Enchant creature
 * Whenever enchanted creature is dealt damage, it deals that much damage to its controller.
 *
 * Ragged Veins' trigger with Pain for All's "it deals" damage. The attached damage trigger names
 * the damaged creature as the triggering entity and stamps its controller as the damage was dealt
 * as the triggering player, so the creature is the source ("it deals" — lifelink and infect apply,
 * per the ruling) and "its controller" is still right when the damage was lethal and both the
 * creature and this Aura are gone by resolution (last-known information).
 */
val SpitefulShadows = card("Spiteful Shadows") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Whenever enchanted creature is dealt damage, it deals that much damage to its controller."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.isDealtDamage()
        effect = Effects.DealDamage(
            amount = DynamicAmounts.triggerDamageAmount(),
            target = EffectTarget.PlayerRef(Player.TriggeringPlayer),
            damageSource = EffectTarget.TriggeringEntity,
        )
        description = "Whenever enchanted creature is dealt damage, it deals that much damage to its controller."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "75"
        artist = "John Stanko"
        flavorText = "\"Let us remind these human cattle why they fear the shadows of this world.\"\n" +
            "—Aldreg, Skirsdag cultist"
        imageUri = "https://cards.scryfall.io/normal/front/e/a/ea0a94fe-11d6-48a7-9195-2cb5eff4b962.jpg?1783940825"
        ruling(
            "2011-01-22",
            "Spiteful Shadows causes the enchanted creature to deal damage to its controller. " +
                "Abilities like lifelink and infect will apply."
        )
    }
}
