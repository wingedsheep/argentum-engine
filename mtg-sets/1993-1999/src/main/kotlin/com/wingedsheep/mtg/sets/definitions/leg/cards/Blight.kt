package com.wingedsheep.mtg.sets.definitions.leg.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Blight
 * {B}{B}
 * Enchantment — Aura
 * Enchant land
 * When enchanted land becomes tapped, destroy it.
 *
 * Kudzu without the re-attach: the attachment trigger binds the enchanted land as the triggering
 * entity, so "it" is [EffectTarget.TriggeringEntity]. The cause-agnostic `becomesTapped()` matches
 * every tap, mana abilities included. Untargeted.
 */
val Blight = card("Blight") {
    manaCost = "{B}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\nWhen enchanted land becomes tapped, destroy it."

    auraTarget = TargetObject(filter = TargetFilter.Land)

    triggeredAbility {
        trigger = Triggers.attached.becomesTapped()
        effect = Effects.Destroy(EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "89"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9ca19b39-4201-463c-bd40-fbffa31c9eda.jpg?1783948069"
    }
}
