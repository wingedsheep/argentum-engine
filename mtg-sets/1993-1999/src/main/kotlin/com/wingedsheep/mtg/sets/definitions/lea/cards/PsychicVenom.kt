package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Psychic Venom
 * {1}{U}
 * Enchantment — Aura
 * Enchant land
 * Whenever enchanted land becomes tapped, this Aura deals 2 damage to that land's controller.
 *
 * The cause-agnostic `becomesTapped()` matches every tap, mana abilities included (per the ruling).
 * The attachment trigger binds the enchanted land as the triggering entity, so "that land's
 * controller" is [EffectTarget.ControllerOfTriggeringEntity] (cf. Artifact Possession).
 */
val PsychicVenom = card("Psychic Venom") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\nWhenever enchanted land becomes tapped, this Aura deals 2 damage to that land's controller."
    auraTarget = TargetObject(filter = TargetFilter.Land)

    triggeredAbility {
        trigger = Triggers.attached.becomesTapped()
        effect = Effects.DealDamage(2, EffectTarget.ControllerOfTriggeringEntity)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "75"
        artist = "Brian Snõddy"
        imageUri = "https://cards.scryfall.io/normal/front/f/3/f3f5b68a-6b0e-431e-89f0-ff60f17687a5.jpg?1783948702"
        ruling("2004-10-04", "Whenever the land is tapped for any reason, the ability triggers.")
    }
}
