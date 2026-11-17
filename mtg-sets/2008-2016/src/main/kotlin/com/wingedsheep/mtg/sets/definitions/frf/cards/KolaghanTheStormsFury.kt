package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Kolaghan, the Storm's Fury — Fate Reforged #155 (canonical printing)
 * {3}{B}{R}
 * Legendary Creature — Dragon
 * 4/5
 * Flying
 * Whenever a Dragon you control attacks, creatures you control get +1/+0 until end of turn.
 * Dash {3}{B}{R}
 *
 * A per-attacker ANY-bound attack trigger (one instance per attacking Dragon, Kolaghan included), so
 * attacking with two Dragons gives creatures you control +2/+0. Each instance pumps the creatures you
 * control as it resolves.
 */
val KolaghanTheStormsFury = card("Kolaghan, the Storm's Fury") {
    manaCost = "{3}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Dragon"
    power = 4
    toughness = 5
    oracleText = "Flying\n" +
        "Whenever a Dragon you control attacks, creatures you control get +1/+0 until end of turn.\n" +
        "Dash {3}{B}{R} (You may cast this spell for its dash cost. If you do, it gains haste, and it's " +
        "returned from the battlefield to its owner's hand at the beginning of the next end step.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withSubtype("Dragon").youControl()).attacks()
        effect = Patterns.Group.modifyStatsForAll(1, 0, GroupFilter.AllCreaturesYouControl)
    }

    dash = "{3}{B}{R}"

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "155"
        artist = "Jaime Jones"
        imageUri = "https://cards.scryfall.io/normal/front/9/6/96376314-2085-4c57-861e-44d25e2d3bc8.jpg?1783938673"
        ruling("2014-11-24", "If you pay the dash cost to cast a creature spell, that card will be returned to its owner's hand only if it's still on the battlefield when its triggered ability resolves. If it dies or goes to another zone before then, it will stay where it is.")
        ruling("2014-11-24", "If a creature enters the battlefield as a copy of or becomes a copy of a creature whose dash cost was paid, the copy won't have haste and won't be returned to its owner's hand.")
    }
}
