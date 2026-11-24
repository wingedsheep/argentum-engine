package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Security Blockade
 * {2}{W}
 * Enchantment — Aura
 *
 * Enchant land
 * When this Aura enters, create a 2/2 white Knight creature token with vigilance.
 * Enchanted land has "{T}: Prevent the next 1 damage that would be dealt to you this turn."
 *
 * The granted ability is the land's own, so "you" is whoever controls (and activates) the land —
 * `EffectTarget.Controller` on the granted ability, the same shield Reinforced Bulwark uses.
 */
val SecurityBlockade = card("Security Blockade") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant land\n" +
        "When this Aura enters, create a 2/2 white Knight creature token with vigilance.\n" +
        "Enchanted land has \"{T}: Prevent the next 1 damage that would be dealt to you this turn.\""

    auraTarget = TargetObject(filter = TargetFilter.Land)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Knight"),
            keywords = setOf(Keyword.VIGILANCE),
            imageUri = "https://cards.scryfall.io/normal/front/6/7/67d3d039-248a-4eb8-be5c-12959b458fea.jpg?1783940383",
        )
        description = "When this Aura enters, create a 2/2 white Knight creature token with vigilance."
    }

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                cost = Costs.Tap,
                effect = Effects.PreventNextDamage(1, EffectTarget.Controller)
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "20"
        artist = "James Ryman"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6250b31-8592-48b2-a877-3637c9ee7d49.jpg?1783940374"
        ruling(
            "2012-10-01",
            "You must target a land to cast Security Blockade. If that land is an illegal target when " +
                "Security Blockade tries to resolve, it won't resolve and won't enter. You won't get the Knight token."
        )
    }
}
