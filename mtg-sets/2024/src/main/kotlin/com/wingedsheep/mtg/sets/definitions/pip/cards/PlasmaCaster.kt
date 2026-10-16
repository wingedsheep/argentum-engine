package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Plasma Caster
 * {1}{R}
 * Artifact — Equipment
 * Equipped creature gets +1/+1.
 * Whenever equipped creature attacks, you get {E}{E} (two energy counters).
 * Pay {E}{E}: Choose target creature that's blocking equipped creature. Flip a coin. If you win
 * the flip, exile the chosen creature. Otherwise, this Equipment deals 1 damage to it.
 * Equip {2}
 *
 * "Blocking equipped creature" (`blockingEntity(EquippedCreature)`) is judged against the creature
 * the Equipment was attached to when the target was chosen, both then and as the ability resolves:
 * moving or losing the Equipment in response doesn't stop it, while a target that has stopped
 * blocking that creature makes the ability fizzle without a flip.
 */
val PlasmaCaster = card("Plasma Caster") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +1/+1.\n" +
        "Whenever equipped creature attacks, you get {E}{E} (two energy counters).\n" +
        "Pay {E}{E}: Choose target creature that's blocking equipped creature. Flip a coin. " +
        "If you win the flip, exile the chosen creature. Otherwise, this Equipment deals 1 damage to it.\n" +
        "Equip {2}"

    staticAbility {
        ability = ModifyStats(1, 1)
    }

    triggeredAbility {
        trigger = Triggers.attached.attacks()
        effect = Effects.GetEnergy(2)
    }

    activatedAbility {
        cost = Costs.PayPlayerCounters(CounterType.ENERGY, 2)
        val blocker = target(
            TargetFilter(GameObjectFilter.Creature.blockingEntity(EffectTarget.EquippedCreature))
        )
        effect = Effects.FlipCoin(
            wonEffect = Effects.Exile(blocker),
            lostEffect = Effects.DealDamage(amount = 1, target = blocker, damageSource = EffectTarget.Self)
        )
        description = "Pay {E}{E}: Choose target creature blocking equipped creature. Flip a coin. " +
            "Win: exile it. Lose: Plasma Caster deals 1 damage to it."
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "64"
        artist = "Kekai Kotaki"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1654256-25ea-4467-812d-25f32fe65cdd.jpg?1783912403"
        ruling(
            "2024-03-08",
            "If the target creature is no longer on the battlefield or no longer blocking the equipped creature when Plasma Caster's third ability tries to resolve, it won't resolve and none of its effects happen. You won't flip a coin."
        )
        ruling(
            "2024-03-08",
            "If Plasma Caster is no longer on the battlefield or no longer attached to the same creature when its third ability resolves, the ability will still resolve as normal."
        )
    }
}
