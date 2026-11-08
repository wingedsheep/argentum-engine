package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantFlashToSpellType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sigarda's Aid — Eldritch Moon #41
 * {W} · Enchantment
 *
 * You may cast Aura and Equipment spells as though they had flash.
 * Whenever an Equipment you control enters, you may attach it to target creature you control.
 *
 * The flash grant is Shimmer Myr's controller-scoped [GrantFlashToSpellType] over an
 * Aura-or-Equipment filter (Halvar, God of Battle's union). The trigger is Puresteel Paladin's
 * "Equipment you control enters"; "it" is the entering Equipment, read through
 * [EffectTarget.TriggeringEntity] as the attachment of the force-attach atom, while the creature is
 * a real target chosen as the trigger goes on the stack. The "may" is decided on resolution; an
 * Equipment that has left the battlefield, or a host it can't legally equip, is a no-op (CR 701.3b).
 */
val SigardasAid = card("Sigarda's Aid") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment"
    oracleText = "You may cast Aura and Equipment spells as though they had flash.\n" +
        "Whenever an Equipment you control enters, you may attach it to target creature you control."

    staticAbility {
        ability = GrantFlashToSpellType(
            filter = GameObjectFilter.Any.withSubtype(Subtype.AURA) or
                GameObjectFilter.Any.withSubtype(Subtype.EQUIPMENT),
            controllerOnly = true
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Artifact.withSubtype(Subtype.EQUIPMENT).youControl()).enters()
        val creature = target(TargetFilter.Creature.youControl())
        effect = Effects.May(
            Effects.AttachTargetEquipmentToCreature(
                equipmentTarget = EffectTarget.TriggeringEntity,
                creatureTarget = creature
            )
        )
        description = "Whenever an Equipment you control enters, you may attach it to target creature you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "41"
        artist = "Howard Lyon"
        flavorText = "The Order of Saint Traft found new strength with Sigarda's support."
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7fc32f1d-dee7-4f71-a652-26a006e6020a.jpg?1783937510"
    }
}
