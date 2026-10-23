package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.RollAdditionalDice
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Barbarian Class {R}
 * Enchantment — Class
 *
 * (Gain the next level as a sorcery to add its ability.)
 * If you would roll one or more dice, instead roll that many dice plus one and ignore the lowest roll.
 *
 * {1}{R}: Level 2
 * Whenever you roll one or more dice, target creature you control gets +2/+0 and gains menace until
 * end of turn.
 *
 * {2}{R}: Level 3
 * Creatures you control have haste.
 */
val BarbarianClass = card("Barbarian Class") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Class"
    oracleText = "(Gain the next level as a sorcery to add its ability.)\n" +
        "If you would roll one or more dice, instead roll that many dice plus one and ignore the lowest roll.\n" +
        "{1}{R}: Level 2 — Whenever you roll one or more dice, target creature you control gets +2/+0 and gains menace until end of turn.\n" +
        "{2}{R}: Level 3 — Creatures you control have haste."

    // Level 1: roll an extra die and ignore the lowest.
    staticAbility {
        ability = RollAdditionalDice(extraDice = 1)
    }

    // Level 2: each roll instruction pumps a creature.
    classLevel(2, "{1}{R}") {
        triggeredAbility {
            trigger = Triggers.you.rollsDice()
            val creature = target(TargetFilter.Creature.youControl())
            effect = Effects.ModifyStats(2, 0, creature) then Effects.GrantKeyword(Keyword.MENACE, creature)
            description = "Whenever you roll one or more dice, target creature you control gets +2/+0 and gains menace until end of turn."
        }
    }

    // Level 3: creatures you control have haste.
    classLevel(3, "{2}{R}") {
        staticAbility {
            ability = GrantKeyword(Keyword.HASTE, GroupFilter.AllCreaturesYouControl)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "131"
        artist = "Campbell White"
        imageUri = "https://cards.scryfall.io/normal/front/6/4/647c2269-bdc7-4455-9158-73abbff6e50e.jpg?1783926485"
        ruling("2021-07-23", "Gaining a level is a normal activated ability. It uses the stack and can be responded to.")
        ruling("2021-07-23", "Gaining a level won't remove abilities that a Class had at a previous level.")
        ruling("2021-07-23", "Some Class cards have an effect that increases when more are under your control. For example, if you have multiple Barbarian Class cards, you roll that many additional dice and ignore that many of the lowest rolls.")
    }
}
