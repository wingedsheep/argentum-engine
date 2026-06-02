package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AttachEquipmentEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Dúnedain Blade
 * {1}{W}
 * Artifact — Equipment
 * Equipped creature gets +2/+1.
 * Equip Human {1}
 * Equip {3}
 */
val DunedainBlade = card("Dúnedain Blade") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+1.\n" +
        "Equip Human {1}\n" +
        "Equip {3} ({3}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = ModifyStats(+2, +1, Filters.EquippedCreature)
    }

    // Equip Human {1}: attach only to a Human creature you control, sorcery speed.
    activatedAbility {
        cost = AbilityCost.Mana(ManaCost.parse("{1}"))
        timing = TimingRule.SorcerySpeed
        val creature = target(
            "Human creature you control",
            TargetCreature(filter = TargetFilter.CreatureYouControl.withSubtype(Subtype.HUMAN))
        )
        effect = AttachEquipmentEffect(creature)
        description = "Equip Human {1}"
    }

    // Equip {3}: attach to any creature you control, sorcery speed.
    equipAbility("{3}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "6"
        artist = "Jarel Threat"
        flavorText = "The sword glinted in the westering sun."
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d5f92bf-e4e7-487a-834c-964fdd6ad674.jpg?1686967688"
    }
}
