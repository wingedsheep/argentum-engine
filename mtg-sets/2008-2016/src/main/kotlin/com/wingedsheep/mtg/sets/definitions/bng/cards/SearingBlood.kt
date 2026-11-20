package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Searing Blood — Born of the Gods #111
 * {R}{R} · Instant
 * Searing Blood deals 2 damage to target creature. When that creature dies this turn, Searing
 * Blood deals 3 damage to the creature's controller.
 *
 * The rider is a watched-entity delayed trigger (Felonious Rage / Devouring Tendrils shape): it
 * fires however that creature dies this turn, not only from this damage. The dying creature is
 * the triggering entity, so [EffectTarget.ControllerOfTriggeringEntity] reads its last-known
 * controller at the moment it died — which may be the caster, or a player who gained control of
 * it after Searing Blood resolved.
 */
val SearingBlood = card("Searing Blood") {
    manaCost = "{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Searing Blood deals 2 damage to target creature. When that creature dies this turn, " +
        "Searing Blood deals 3 damage to the creature's controller."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.DealDamage(2, creature) then
            Effects.CreateDelayedTrigger(
                effect = Effects.DealDamage(3, EffectTarget.ControllerOfTriggeringEntity),
                trigger = Triggers.self.dies(),
                watchedTarget = creature,
                fireOnce = true,
                expiry = DelayedTriggerExpiry.EndOfTurn,
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "111"
        artist = "Daniel Ljunggren"
        flavorText = "Purphoros's blessing: sometimes a boon, sometimes a bane. Always ablaze."
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bb43fd07-d281-447d-88bf-c53498c2cf20.jpg?1783939539"
    }
}
