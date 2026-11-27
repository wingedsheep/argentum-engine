package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Aggravate — Avacyn Restored #125 (canonical printing)
 * {3}{R}{R} · Instant
 *
 * Aggravate deals 1 damage to each creature target player controls. Each creature dealt damage
 * this way attacks this turn if able.
 *
 * The group damage is one simultaneous event, and `storeDamagedAs` collects only what the damage
 * actually reached — a creature whose damage was prevented (protection from red, a shield) is not
 * forced to attack. Damage redirected to a player lands in the collection too, so it is narrowed to
 * creatures before the must-attack mark. A creature that dies to the 1 damage simply isn't around
 * to attack.
 */
val Aggravate = card("Aggravate") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Aggravate deals 1 damage to each creature target player controls. " +
        "Each creature dealt damage this way attacks this turn if able."

    spell {
        val player = target(Targets.Player)
        effect = Effects.Pipeline {
            val damaged = runStoringCollection {
                Effects.ForEachInGroup(
                    GroupFilter(GameObjectFilter.Creature.targetPlayerControls(player)),
                    Effects.DealDamage(1, EffectTarget.IterationEntity, storeDamagedAs = it),
                )
            }
            val creatures = filter(damaged, GameObjectFilter.Creature)
            run(Effects.ForEachInCollection(creatures, Effects.MarkMustAttackThisTurn(EffectTarget.IterationEntity)))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "125"
        artist = "Matt Stewart"
        imageUri = "https://cards.scryfall.io/normal/front/9/9/999f40a7-b723-42e1-83c1-f45a72a26dd4.jpg?1783940693"
        ruling(
            "2012-05-01",
            "Aggravate targets only the player. Creatures with hexproof that player controls will be dealt " +
                "damage by Aggravate if it resolves."
        )
        ruling(
            "2012-05-01",
            "The controller of a creature dealt damage by Aggravate still decides which player or planeswalker " +
                "the creature attacks."
        )
        ruling(
            "2012-05-01",
            "If, during a player's declare attackers step, a creature is tapped, is affected by a spell or " +
                "ability that says it can't attack, or hasn't been under that player's control continuously " +
                "since the turn began (and doesn't have haste), then it doesn't attack. If there's a cost " +
                "associated with having a creature attack, the player isn't forced to pay that cost, so it " +
                "doesn't have to attack in that case either."
        )
    }
}
