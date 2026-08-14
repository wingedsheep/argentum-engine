package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityId
import com.wingedsheep.sdk.scripting.ActivatedAbility
import com.wingedsheep.sdk.scripting.GrantActivatedAbility
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Viridian Longbow — Mirrodin #270
 * {1} · Artifact — Equipment
 *
 * Equipped creature has "{T}: This creature deals 1 damage to any target."
 * Equip {3}
 *
 * The Sorcerer's Wand shape: a [GrantActivatedAbility] static handing the host a `{T}` pinger.
 * Two consequences of the grant living on the *host* (CR 113.7) matter here and both fall out of
 * [EffectTarget.Self] resolving to the equipped creature inside a granted ability: the `{T}` cost
 * taps the creature (so summoning sickness gates it, and it can't both attack and shoot), and the
 * creature — not the Longbow — is the source of the damage, which is what makes this the classic
 * deathtouch-and-Longbow combo.
 */
val ViridianLongbow = card("Viridian Longbow") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature has \"{T}: This creature deals 1 damage to any target.\"\n" +
        "Equip {3}"

    staticAbility {
        ability = GrantActivatedAbility(
            ability = ActivatedAbility(
                id = AbilityId.generate(),
                cost = Costs.Tap,
                effect = Effects.DealDamage(
                    amount = 1,
                    target = EffectTarget.ContextTarget(0),
                    damageSource = EffectTarget.Self
                ),
                targetRequirements = listOf(AnyTarget())
            )
        )
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "270"
        artist = "Jeremy Jarvis"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be892d73-d1f4-4c36-b674-01ae21ff1484.jpg?1783944497"
    }
}
