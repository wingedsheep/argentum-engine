package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rampaging Raptor — March of the Machine #160
 * {2}{R}{R} · Creature — Dinosaur · 4/4
 *
 * Trample, haste
 * {2}{R}: This creature gets +2/+0 until end of turn.
 * Whenever this creature deals combat damage to an opponent, it deals that much damage to target
 * planeswalker that player controls or battle that player protects.
 *
 * - "to an opponent", not "to a player" ([Recipient.Opponent]) — per the 2023-04-14 ruling, damage
 *   redirected to you doesn't trigger it.
 * - "that player" is the damaged opponent, resolved through `ControlledByTriggeringPlayer`. For the
 *   planeswalker half it reads the controller; for the battle half it reads the battle's *protector*
 *   via `protectedBy(...)` (CR 310.9e) — a Siege you cast is controlled by you but protected by the
 *   opponent, so a controller predicate would pick the wrong battles.
 * - "that much" is the combat damage dealt to the opponent (`triggerDamageAmount()`); leaving
 *   `damageSource` unset attributes the damage to this creature.
 */
val RampagingRaptor = card("Rampaging Raptor") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dinosaur"
    oracleText = "Trample, haste\n" +
        "{2}{R}: This creature gets +2/+0 until end of turn.\n" +
        "Whenever this creature deals combat damage to an opponent, it deals that much damage to " +
        "target planeswalker that player controls or battle that player protects."
    power = 4
    toughness = 4

    keywords(Keyword.TRAMPLE, Keyword.HASTE)

    activatedAbility {
        cost = Costs.Mana("{2}{R}")
        effect = Effects.ModifyStats(2, 0, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.Opponent)
        val t = target(
            TargetFilter(
                GameObjectFilter.Planeswalker.controlledByTriggeringPlayer() or
                    GameObjectFilter.Battle.protectedBy(ControllerPredicate.ControlledByTriggeringPlayer)
            )
        )
        effect = Effects.DealDamage(DynamicAmounts.triggerDamageAmount(), t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "160"
        artist = "Denys Tsiperko"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/64b80ddb-ce55-4ebc-b587-77843abc8bad.jpg?1783916985"
        ruling(
            "2023-04-14",
            "Unlike many similar abilities, Rampaging Raptor's triggered ability triggers whenever " +
                "it deals combat damage to an opponent, not to a player. If it happens to deal " +
                "combat damage to you (usually due to a redirection effect, which would be unusual), " +
                "the ability won't trigger."
        )
    }
}
