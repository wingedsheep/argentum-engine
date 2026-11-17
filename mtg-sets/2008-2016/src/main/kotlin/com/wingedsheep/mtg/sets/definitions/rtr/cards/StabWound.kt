package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Stab Wound
 * {2}{B}
 * Enchantment — Aura
 * Enchant creature
 * Enchanted creature gets -2/-2.
 * At the beginning of the upkeep of enchanted creature's controller, that player loses 2 life.
 */
val StabWound = card("Stab Wound") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Enchanted creature gets -2/-2.\n" +
        "At the beginning of the upkeep of enchanted creature's controller, that player loses 2 life."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    staticAbility {
        ability = ModifyStats(-2, -2, Filters.EnchantedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.beginningOf(Step.UPKEEP)
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "78"
        artist = "Scott Chou"
        imageUri = "https://cards.scryfall.io/normal/front/7/b/7b562269-e6ec-4f8d-844e-26b272248d9d.jpg"
    }
}
