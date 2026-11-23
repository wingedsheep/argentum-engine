package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chemister's Trick — Return to Ravnica #149 (canonical printing)
 * {U}{R} · Instant
 *
 * Target creature you don't control gets -2/-0 until end of turn and attacks this turn if able.
 * Overload {3}{U}{R}
 *
 * Blustersquall's overload shape. The overloaded version walks the creatures you don't control
 * at resolution ([Effects.ForEachInGroup]), so — per the ruling — creatures that come under
 * another player's control later in the turn are unaffected.
 */
val ChemistersTrick = card("Chemister's Trick") {
    manaCost = "{U}{R}"
    colorIdentity = "UR"
    typeLine = "Instant"
    oracleText = "Target creature you don't control gets -2/-0 until end of turn and attacks this turn if able.\n" +
        "Overload {3}{U}{R} (You may cast this spell for its overload cost. If you do, change \"target\" " +
        "in its text to \"each.\")"

    keywordAbility(KeywordAbility.overload("{3}{U}{R}"))

    spell {
        val notYours = GameObjectFilter.Creature.withControllerPredicate(
            ControllerPredicate.Not(ControllerPredicate.ControlledByYou)
        )
        val creature = target(TargetFilter(notYours))
        effect = Effects.ModifyStats(-2, 0, creature) then Effects.MarkMustAttackThisTurn(creature)

        overloadEffect = Effects.ForEachInGroup(
            GroupFilter(notYours),
            Effects.ModifyStats(-2, 0, EffectTarget.IterationEntity) then
                Effects.MarkMustAttackThisTurn(EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "149"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/normal/front/d/b/dbfc2748-351f-4b5d-8a7e-bc51851578bb.jpg?1783940342"
        ruling(
            "2012-10-01",
            "The controller of a creature that attacks if able still chooses which player or planeswalker it attacks."
        )
        ruling(
            "2012-10-01",
            "If, during a player's declare attacker's step, a creature is tapped, is affected by a spell or " +
                "ability that says it can't attack, or hasn't been under that player's control continuously " +
                "since the turn began (and doesn't have haste), then it doesn't attack. If there's a cost " +
                "associated with having a creature attack, the player isn't forced to pay that cost, so it " +
                "doesn't have to attack in that case either."
        )
        ruling(
            "2012-10-01",
            "If you cast Chemister's Trick with overload, only creatures you don't control that are on the " +
                "battlefield when Chemister's Trick resolves are affected. Creatures that come under another " +
                "player's control later in the turn are not."
        )
        ruling(
            "2024-01-12",
            "Because a spell with overload doesn't target when its overload cost is paid, it may affect " +
                "permanents with hexproof or with protection from the appropriate color."
        )
    }
}
