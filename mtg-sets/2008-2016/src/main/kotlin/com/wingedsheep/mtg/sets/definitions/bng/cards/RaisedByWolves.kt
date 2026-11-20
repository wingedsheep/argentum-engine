package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Raised by Wolves
 * {3}{G}{G}
 * Enchantment — Aura
 * Enchant creature
 * When this Aura enters, create two 2/2 green Wolf creature tokens.
 * Enchanted creature gets +1/+1 for each Wolf you control.
 *
 * "Each Wolf you control" counts every Wolf permanent (the enchanted creature too, if it is
 * one), read from projected state during layer 7c so type-changing effects are seen. "You" is
 * the Aura's controller.
 */
val RaisedByWolves = card("Raised by Wolves") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, create two 2/2 green Wolf creature tokens.\n" +
        "Enchanted creature gets +1/+1 for each Wolf you control."
    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Wolf"),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/8/9/8958b650-c831-4499-813b-5699954ab2f0.jpg?1783939515"
        )
        description = "When this Aura enters, create two 2/2 green Wolf creature tokens."
    }

    staticAbility {
        ability = GrantDynamicStats(
            filter = GroupFilter.attachedCreature(),
            powerBonus = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Permanent.withSubtype(Subtype.WOLF)
            ).count(),
            toughnessBonus = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Permanent.withSubtype(Subtype.WOLF)
            ).count()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "135"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bcfaace7-86e2-427e-97b1-b09dfdd30fee.jpg?1783939529"
    }
}
