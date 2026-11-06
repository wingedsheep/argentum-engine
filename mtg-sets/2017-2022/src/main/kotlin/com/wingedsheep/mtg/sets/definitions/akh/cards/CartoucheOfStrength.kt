package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Cartouche of Strength
 * {2}{G}
 * Enchantment — Aura Cartouche
 * Enchant creature you control
 * When this Aura enters, you may have enchanted creature fight target creature an opponent controls.
 * Enchanted creature gets +1/+1 and has trample.
 *
 * The printed "you may" is the resolution-time yes/no (`optional = true`); the target is still
 * chosen on announcement. If the Aura has left by resolution, [EffectTarget.EnchantedCreature]
 * falls back to the creature it last enchanted, matching the 2017-04-18 ruling.
 */
val CartoucheOfStrength = card("Cartouche of Strength") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura Cartouche"
    oracleText = "Enchant creature you control\n" +
        "When this Aura enters, you may have enchanted creature fight target creature an opponent controls. " +
        "(Each deals damage equal to its power to the other.)\n" +
        "Enchanted creature gets +1/+1 and has trample."

    auraTarget = TargetObject(filter = TargetFilter.CreatureYouControl)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val theirs = target(TargetFilter.CreatureOpponentControls)
        optional = true
        effect = Effects.Fight(EffectTarget.EnchantedCreature, theirs)
    }

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.TRAMPLE)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "158"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6bb237c-4e39-4879-90b4-2f507a90d3d7.jpg?1783936478"
        ruling("2017-04-18", "If the triggered ability's target is illegal when it tries to resolve, or if the enchanted creature has left the battlefield, no creature will deal or be dealt damage.")
        ruling("2017-04-18", "If Cartouche of Strength leaves the battlefield before its triggered ability resolves, the creature it last enchanted will fight the target creature if it's still on the battlefield at that time. However, it won't have +1/+1 from Cartouche of Strength anymore.")
    }
}
