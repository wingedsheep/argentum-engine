package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostGating
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Paladin Class {W}
 * Enchantment — Class
 *
 * (Gain the next level as a sorcery to add its ability.)
 * Spells your opponents cast during your turn cost {1} more to cast.
 *
 * {2}{W}: Level 2
 * Creatures you control get +1/+1.
 *
 * {4}{W}: Level 3
 * Whenever you attack, until end of turn, target attacking creature gets +1/+1 for each other
 * attacking creature and gains double strike.
 *
 * Level 1 is an `OpponentsCast` tax gated by `OnlyIf(IsYourTurn)`; the gate reads "your" as the
 * Class's controller, not the opponent casting the spell. Level 3's bonus is the attacking-creature
 * count less the target itself, fixed as the trigger resolves; a lone attacker gets +0/+0 and
 * double strike (2021-07-23 ruling).
 */
val PaladinClass = card("Paladin Class") {
    manaCost = "{W}"
    colorIdentity = "W"
    typeLine = "Enchantment — Class"
    oracleText = "(Gain the next level as a sorcery to add its ability.)\n" +
        "Spells your opponents cast during your turn cost {1} more to cast.\n" +
        "{2}{W}: Level 2 — Creatures you control get +1/+1.\n" +
        "{4}{W}: Level 3 — Whenever you attack, until end of turn, target attacking creature gets " +
        "+1/+1 for each other attacking creature and gains double strike."

    // Level 1: tax opponents' spells during your turn.
    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.OpponentsCast(GameObjectFilter.Any),
            modification = CostModification.IncreaseGeneric(1),
            gating = CostGating.OnlyIf(Conditions.IsYourTurn),
        )
    }

    // Level 2: creatures you control get +1/+1.
    classLevel(2, "{2}{W}") {
        staticAbility {
            ability = ModifyStats(1, 1, GroupFilter.AllCreaturesYouControl)
        }
    }

    // Level 3: whenever you attack, pump one attacker by the other attackers and give it double strike.
    classLevel(3, "{4}{W}") {
        triggeredAbility {
            trigger = Triggers.you.attacks()
            val attacker = target(TargetFilter.AttackingCreature)
            val others = DynamicAmounts.battlefield(Player.Each, GameObjectFilter.Creature.attacking()).count() - 1
            effect = Effects.ModifyStats(others, others, attacker) then
                Effects.GrantKeyword(Keyword.DOUBLE_STRIKE, attacker)
            description = "Whenever you attack, until end of turn, target attacking creature gets +1/+1 " +
                "for each other attacking creature and gains double strike."
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "29"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5bf81fb1-7992-4ae9-b1a8-80c31579a2bf.jpg?1783926526"
        ruling("2021-07-23", "Additional costs from Paladin Class cards are cumulative. For example, if you control two Paladin Class cards, spells your opponents cast cost {2} more to cast.")
        ruling("2021-07-23", "If Paladin Class is level 3 and you attack with exactly one creature, it won't get a power and toughness bonus, but it will get double strike.")
        ruling("2021-07-23", "Gaining a level is a normal activated ability. It uses the stack and can be responded to.")
        ruling("2021-07-23", "Gaining a level won't remove abilities that a Class had at a previous level.")
    }
}
