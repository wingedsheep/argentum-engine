package com.wingedsheep.mtg.sets.definitions.cmd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Champion's Helm — Commander #244
 * {3} · Artifact — Equipment · Rare
 *
 * Equipped creature gets +2/+2.
 * As long as equipped creature is legendary, it has hexproof.
 * Equip {1}
 *
 * The hexproof rider is a static gated by [Conditions.EntityMatches] on
 * [EffectTarget.EquippedCreature], tracking the equipped creature's projected supertype.
 */
val ChampionsHelm = card("Champion's Helm") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+2.\n" +
        "As long as equipped creature is legendary, it has hexproof. " +
        "(It can't be the target of spells or abilities your opponents control.)\n" +
        "Equip {1}"

    staticAbility {
        ability = ModifyStats(2, 2, Filters.EquippedCreature)
    }
    staticAbility {
        condition = Conditions.EntityMatches(
            EffectTarget.EquippedCreature,
            GameObjectFilter.Creature.legendary()
        )
        ability = GrantKeyword(Keyword.HEXPROOF)
    }
    equipAbility("{1}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "244"
        artist = "Alan Pollack"
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dcad6846-0b35-4193-b647-16e597357f9b.jpg?1783941160"
        ruling(
            "2011-09-22",
            "It's possible for a Champion's Helm you control to be attached to a creature an opponent controls " +
                "(if an opponent gains control of your equipped creature, for example). In this case, the creature " +
                "couldn't be the target of spells or abilities you control, as you are an opponent of the " +
                "controller of the creature with hexproof."
        )
    }
}
