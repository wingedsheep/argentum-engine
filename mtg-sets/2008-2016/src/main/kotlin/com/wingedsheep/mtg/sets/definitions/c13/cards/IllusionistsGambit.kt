package com.wingedsheep.mtg.sets.definitions.c13.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackYou
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Illusionist's Gambit
 * {2}{U}{U}
 * Instant
 *
 * Cast this spell only during the declare blockers step on an opponent's turn.
 * Remove all attacking creatures from combat and untap them. After this phase, there is an
 * additional combat phase. Each of those creatures attacks that combat if able. They can't attack
 * you or planeswalkers you control that combat.
 *
 * The attacking creatures are frozen once by the group loop, so "those creatures" stays exactly the
 * set that was attacking at resolution — each is removed from combat, untapped, marked to attack if
 * able, and given a [CantAttackYou] restriction whose "you" is this spell's controller (the grant
 * records it). The restriction lasts until end of turn: the added combat is the turn's last, and a
 * creature with no legal defender left is not a valid attacker, so "attacks if able" never forces an
 * impossible attack (two-player, it simply can't attack at all). Creatures that weren't attacking are
 * untouched and may attack anyone (ruling 2013-10-17).
 */
val IllusionistsGambit = card("Illusionist's Gambit") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Cast this spell only during the declare blockers step on an opponent's turn.\n" +
        "Remove all attacking creatures from combat and untap them. After this phase, there is an " +
        "additional combat phase. Each of those creatures attacks that combat if able. They can't " +
        "attack you or planeswalkers you control that combat."

    spell {
        castOnlyDuring(Step.DECLARE_BLOCKERS)
        castOnlyIf(Conditions.IsOpponentsTurn)
        effect = Effects.ForEachInGroup(
            GroupFilter.AttackingCreatures,
            Effects.RemoveFromCombat(EffectTarget.IterationEntity) then
                Effects.Untap(EffectTarget.IterationEntity) then
                Effects.MarkMustAttackThisTurn(EffectTarget.IterationEntity) then
                Effects.GrantStaticAbility(
                    CantAttackYou(coversPlaneswalkers = true),
                    EffectTarget.IterationEntity,
                    Duration.EndOfTurn
                )
        ) then Effects.AddCombatPhase
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "47"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a00cc6ab-e97c-4eba-bc7e-49d77690433b.jpg?1783939682"
        ruling(
            "2013-10-17",
            "Creatures that didn't attack during the combat phase when Illusionist's Gambit resolved " +
                "aren't required to attack in the additional combat phase, although they may. Those " +
                "creatures can attack you or a planeswalker you control."
        )
    }
}
