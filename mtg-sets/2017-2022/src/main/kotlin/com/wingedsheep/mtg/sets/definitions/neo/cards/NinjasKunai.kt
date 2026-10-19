package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Ninja's Kunai
 * {1}
 * Artifact — Equipment — Common (NEO #252)
 *
 * "Equipped creature has "{1}, {T}, Sacrifice Ninja's Kunai: Ninja's Kunai deals 3 damage to any target."
 *  Equip {1}"
 *
 * The Blazing Torch idiom: the quoted ability is granted to the bearer ([GrantActivatedAbility]), so
 * `{T}` taps the creature; "Sacrifice Ninja's Kunai" is [Costs.SacrificeGrantingPermanent] (which also
 * enforces the ruling that a player who controls the creature but not the Kunai can't activate it),
 * and the Kunai — not the creature — is the damage source via [EffectTarget.GrantingSource].
 */
val NinjasKunai = card("Ninja's Kunai") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature has \"{1}, {T}, Sacrifice Ninja's Kunai: Ninja's Kunai deals 3 damage to any target.\"\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    // Equipped creature has "{1}, {T}, Sacrifice Ninja's Kunai: Ninja's Kunai deals 3 damage to any target."
    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Composite(
                    Costs.Mana("{1}"),
                    Costs.Tap,
                    Costs.SacrificeGrantingPermanent
                )
                val anyTarget = target(Targets.Any)
                effect = Effects.DealDamage(
                    amount = 3,
                    target = anyTarget,
                    damageSource = EffectTarget.GrantingSource
                )
                description = "{1}, {T}, Sacrifice Ninja's Kunai: Ninja's Kunai deals 3 damage to any target."
            },
            filter = Filters.EquippedCreature
        )
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "252"
        artist = "Tomasz Jedruszek"
        flavorText = "A point to pierce flesh and spirit alike."
        imageUri = "https://cards.scryfall.io/normal/front/a/f/af6ae567-c77a-416a-b609-4df3d0dd2e18.jpg?1783923824"
        ruling(
            "2022-02-18",
            "If a player gains control of a creature equipped with an opponent's Ninja's Kunai without also " +
                "gaining control of the Equipment, they cannot activate the ability it grants the equipped " +
                "creature because you cannot sacrifice permanents you don't control."
        )
    }
}
