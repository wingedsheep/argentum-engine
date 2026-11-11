package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Visions of Brutality
 * {1}{B}
 * Enchantment — Aura
 * Devoid (This card has no color.)
 * Enchant creature
 * Enchanted creature can't block.
 * Whenever enchanted creature deals damage, its controller loses that much life.
 *
 * "Its controller" is the enchanted creature's controller — last-known if the creature died to
 * the damage it dealt. That damage still triggers the Aura: it triggered as the damage was dealt
 * (CR 603.2), before state-based actions put the creature and the Aura into the graveyard.
 */
val VisionsOfBrutality = card("Visions of Brutality") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Devoid (This card has no color.)\n" +
        "Enchant creature\n" +
        "Enchanted creature can't block.\n" +
        "Whenever enchanted creature deals damage, its controller loses that much life."
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    keywords(Keyword.DEVOID)

    staticAbility {
        ability = CantBlock(filter = GroupFilter.attachedCreature())
    }

    triggeredAbility {
        trigger = Triggers.attached.dealsDamage()
        effect = Effects.LoseLife(DynamicAmounts.triggerDamageAmount(), EffectTarget.ControllerOfDamageSource)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "81"
        artist = "Jama Jurabaev"
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec9ed374-54ad-4b81-b071-0b4668feb282.jpg?1783937914"
    }
}
