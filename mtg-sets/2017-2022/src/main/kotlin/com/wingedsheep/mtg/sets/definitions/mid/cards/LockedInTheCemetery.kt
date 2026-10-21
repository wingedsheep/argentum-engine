package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Locked in the Cemetery
 * {1}{U}
 * Enchantment — Aura
 * Enchant creature
 * When this Aura enters, if there are five or more cards in your graveyard, tap enchanted creature.
 * Enchanted creature doesn't untap during its controller's untap step.
 */
val LockedInTheCemetery = card("Locked in the Cemetery") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, if there are five or more cards in your graveyard, tap enchanted creature.\n" +
        "Enchanted creature doesn't untap during its controller's untap step."
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.CardsInGraveyardAtLeast(5)
        effect = Effects.Tap(EffectTarget.EnchantedCreature)
    }

    staticAbility {
        ability = GrantKeyword(AbilityFlag.DOESNT_UNTAP.name)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "60"
        artist = "Tran Nguyen"
        flavorText = "\"Help! I'm not a zombie! Let me out!\""
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68593c6d-8d1b-4c36-84a1-f1144669825e.jpg"
    }
}
