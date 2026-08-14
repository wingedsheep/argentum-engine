package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Shatter the Oath
 * {3}{B}{B}
 * Sorcery
 *
 * Destroy target creature or enchantment. Create a Wicked Role token attached to up to one target
 * creature you control. (If you control another Role on it, put that one into the graveyard.
 * Enchanted creature gets +1/+1. When this token is put into a graveyard, each opponent loses 1
 * life.)
 *
 * Two independent target requirements, same shape as Cut In: a mandatory "target creature or
 * enchantment" that is destroyed, and an optional ("up to one") creature you control that gains the
 * Wicked Role. Per the Scryfall ruling, declining the second target — or having it become illegal —
 * only costs the Role token; the destruction still happens, because each target is checked
 * independently and the spell resolves as long as at least one target is still legal (CR 608.2b).
 *
 * The Wicked Role's own "when this token is put into a graveyard, each opponent loses 1 life"
 * trigger lives on the predefined token, so nothing about the drain needs restating here.
 */
val ShatterTheOath = card("Shatter the Oath") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target creature or enchantment. Create a Wicked Role token attached to " +
        "up to one target creature you control. (If you control another Role on it, put that one " +
        "into the graveyard. Enchanted creature gets +1/+1. When this token is put into a " +
        "graveyard, each opponent loses 1 life.)"

    spell {
        val doomed = target("target creature or enchantment", Targets.CreatureOrEnchantment)
        val roleTarget = target(
            "up to one target creature you control",
            TargetCreature(optional = true, filter = TargetFilter.CreatureYouControl),
        )
        effect = Effects.Composite(
            Effects.Destroy(doomed),
            Effects.CreateRoleToken("Wicked Role", roleTarget),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "106"
        artist = "Dominik Mayer"
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc79f0f7-0a09-4a74-b2b9-cc1ce608d89f.jpg?1783915102"

        ruling(
            "2023-09-01",
            "If you don't choose a second target for Shatter the Oath or that target is illegal as " +
                "the spell resolves, the Wicked Role token won't be created."
        )
        ruling(
            "2023-09-01",
            "If a permanent has more than one Role attached to it controlled by the same player, " +
                "each of those Roles except the one with the most recent timestamp is put into its " +
                "owner's graveyard. This is a state-based action."
        )
        ruling(
            "2023-09-01",
            "A permanent can have multiple Roles attached to it if each one is controlled by a " +
                "different player."
        )
    }
}
