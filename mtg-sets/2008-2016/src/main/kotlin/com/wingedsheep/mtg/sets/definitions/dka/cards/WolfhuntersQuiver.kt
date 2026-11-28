package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wolfhunter's Quiver
 * {1}
 * Artifact — Equipment
 * Equipped creature has "{T}: This creature deals 1 damage to any target" and
 * "{T}: This creature deals 3 damage to target Werewolf creature."
 * Equip {5}
 *
 * The Viridian Longbow shape twice over: each granted ability lives on the host, so the `{T}` taps
 * the equipped creature and [EffectTarget.Self] makes that creature the damage source.
 */
val WolfhuntersQuiver = card("Wolfhunter's Quiver") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText =
        "Equipped creature has \"{T}: This creature deals 1 damage to any target\" and " +
            "\"{T}: This creature deals 3 damage to target Werewolf creature.\"\n" +
            "Equip {5}"

    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Tap
                val anyTarget = target(Targets.Any)
                effect = Effects.DealDamage(
                    amount = 1,
                    target = anyTarget,
                    damageSource = EffectTarget.Self,
                )
            }
        )
    }
    staticAbility {
        ability = GrantActivatedAbility(
            ability = grantedActivatedAbility {
                cost = Costs.Tap
                val werewolf = target(TargetFilter(GameObjectFilter.Creature.withSubtype(Subtype.WEREWOLF)))
                effect = Effects.DealDamage(
                    amount = 3,
                    target = werewolf,
                    damageSource = EffectTarget.Self,
                )
            }
        )
    }

    equipAbility("{5}")

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "154"
        artist = "Daniel Ljunggren"
        imageUri =
            "https://cards.scryfall.io/normal/front/d/8/d84c9b19-9b4d-4a60-984f-636b749c8bcc.jpg?1783940790"
    }
}
