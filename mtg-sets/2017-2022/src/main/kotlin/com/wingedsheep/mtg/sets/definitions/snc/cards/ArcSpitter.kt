package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Arc Spitter — Streets of New Capenna #233
 * {1} · Artifact — Equipment
 *
 * Equipped creature has "{1}: This creature deals 1 damage to target creature that's blocking it."
 * Equip {1}
 *
 * The Viridian Longbow shape: a [GrantActivatedAbility] static whose ability lives on the host
 * (CR 113.7), so inside it the ability's source — and [EffectTarget.Self] — is the equipped
 * creature. That makes the source-relative `blockingSource()` target filter read "blocking the
 * equipped creature", and the creature (not the Equipment) the source of the damage.
 */
val ArcSpitter = card("Arc Spitter") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature has \"{1}: This creature deals 1 damage to target creature " +
        "that's blocking it.\"\n" +
        "Equip {1} ({1}: Attach to target creature you control. Equip only as a sorcery.)"

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Mana("{1}")
                val blocker = target(TargetFilter(GameObjectFilter.Creature.blockingSource()))
                effect = Effects.DealDamage(
                    amount = 1,
                    target = blocker,
                    damageSource = EffectTarget.Self
                )
                description = "{1}: This creature deals 1 damage to target creature that's blocking it."
            }
        )
    }

    equipAbility("{1}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "233"
        artist = "Matt Forsyth"
        flavorText = "\"I'm fresh out of mercy today. Why don't you ask Sparky here for forgiveness?\""
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f95a00ed-dc77-4dcd-9e14-6902565974b5.jpg?1783923071"
    }
}
