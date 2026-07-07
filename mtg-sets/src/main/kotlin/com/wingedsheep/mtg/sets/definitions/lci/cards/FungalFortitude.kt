package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fungal Fortitude
 * {1}{B}
 * Enchantment — Aura
 * Common (LCI #106)
 *
 * Flash
 * Enchant creature
 * Enchanted creature gets +2/+0.
 * When enchanted creature dies, return it to the battlefield tapped under its owner's control.
 */
val FungalFortitude = card("Fungal Fortitude") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Flash\nEnchant creature\nEnchanted creature gets +2/+0.\nWhen enchanted creature dies, return it to the battlefield tapped under its owner's control."

    keywords(Keyword.FLASH)

    auraTarget = Targets.Creature

    // "Enchanted creature gets +2/+0" — Layer 7c (POWER_TOUGHNESS, MODIFY)
    staticAbility {
        ability = ModifyStats(2, 0)
    }

    // "When enchanted creature dies, return it to the battlefield tapped under its owner's control."
    // trigger: enchanted creature leaves battlefield to graveyard (TriggerBinding.ATTACHED watches the host)
    // effect: PutOntoBattlefield without controllerOverride defaults to the card's owner's control
    triggeredAbility {
        trigger = Triggers.leavesBattlefield(to = Zone.GRAVEYARD, binding = TriggerBinding.ATTACHED)
        effect = Effects.PutOntoBattlefield(EffectTarget.TriggeringEntity, tapped = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "106"
        artist = "Izzy"
        imageUri = "https://cards.scryfall.io/normal/front/9/d/9d2bd0ca-521c-45d9-85ed-2f36f583408e.jpg?1782694527"
    }
}
