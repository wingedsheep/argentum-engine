package com.wingedsheep.mtg.sets.definitions.m11.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.PlayersCantCastSpells
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Angelic Arbiter
 * {5}{W}{W}
 * Creature — Angel
 * 5/6
 *
 * Flying
 * Each opponent who cast a spell this turn can't attack with creatures.
 * Each opponent who attacked with a creature this turn can't cast spells.
 *
 * The attack half restricts opponents' creatures whose controller ("its controller", the player
 * attacking with it — right for Two-Headed Giant teammates too) has cast a spell this turn. The
 * cast half reads "you attacked this turn" from each caster's seat via `conditionFromCaster`; it
 * is the per-player attack record, so an attacking token that has since died still counts. Both
 * read the whole turn's history, so an Arbiter that arrives mid-turn sees what the opponent
 * already did (ruling 2010-08-15).
 */
val AngelicArbiter = card("Angelic Arbiter") {
    manaCost = "{5}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 5
    toughness = 6
    oracleText = "Flying\n" +
        "Each opponent who cast a spell this turn can't attack with creatures.\n" +
        "Each opponent who attacked with a creature this turn can't cast spells."

    keywords(Keyword.FLYING)

    staticAbility {
        ability = CantAttackUnless(
            condition = Conditions.CompareAmounts(
                DynamicAmounts.spellsCastThisTurn(Player.ControllerOfIterationEntity),
                ComparisonOperator.EQ, 0
            ),
            filter = GroupFilter.AllCreaturesOpponentsControl
        )
    }

    staticAbility {
        ability = PlayersCantCastSpells(
            affected = Player.EachOpponent,
            condition = Conditions.YouAttackedThisTurn,
            conditionFromCaster = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "4"
        artist = "Steve Argyle"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/71f05793-9e6f-4815-9a13-f791cb4b6aa0.jpg?1783941838"

        ruling("2010-08-15", "During your turn, Angelic Arbiter's abilities have no effect on the game.")
        ruling("2010-08-15", "During an opponent's turn, that opponent may either cast spells or attack with creatures, but not both (assuming that Angelic Arbiter is on the battlefield for the entirety of that turn). The player may perform other actions, such as activating abilities and playing lands.")
        ruling("2010-08-15", "If Angelic Arbiter leaves the battlefield during an opponent's turn, its abilities cease to affect the game. For example, if an opponent casts Doom Blade to destroy Angelic Arbiter, that player may then attack with creatures.")
        ruling("2010-08-15", "If Angelic Arbiter enters during an opponent's turn, its abilities take actions that player performed earlier in the turn into account, even though it wasn't on the battlefield at the time. For example, if an opponent casts a spell, then you use Leyline of Anticipation's ability to cast Angelic Arbiter as though it had flash, that player won't be able to attack with creatures that turn.")
        ruling("2010-08-15", "Angelic Arbiter's last ability applies if an opponent attacked with at least one creature during the current turn.")
    }
}
