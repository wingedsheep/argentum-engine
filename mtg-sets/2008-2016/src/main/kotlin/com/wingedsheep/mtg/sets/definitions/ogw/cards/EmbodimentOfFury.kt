package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Embodiment of Fury — Oath of the Gatewatch #107
 * {3}{R} · Creature — Elemental · 4/3
 *
 * Trample
 * Land creatures you control have trample.
 * Landfall — Whenever a land you control enters, you may have target land you control become a
 * 3/3 Elemental creature with haste until end of turn. It's still a land.
 *
 * The target is chosen when the trigger goes on the stack; the "you may" is asked on resolution.
 * [Effects.BecomeCreature] adds the creature type and Elemental subtype on top of the land's own
 * types, so it keeps its land types and mana abilities. The trample grant reads projected state, so
 * the land this animates (or any other land creature) has trample while it's a creature.
 */
val EmbodimentOfFury = card("Embodiment of Fury") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    power = 4
    toughness = 3
    oracleText = "Trample\n" +
        "Land creatures you control have trample.\n" +
        "Landfall — Whenever a land you control enters, you may have target land you control become " +
        "a 3/3 Elemental creature with haste until end of turn. It's still a land."

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = GrantKeyword(
            Keyword.TRAMPLE,
            GroupFilter(GameObjectFilter.Creature and GameObjectFilter.Land.youControl()),
        )
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        val land = target(TargetFilter.LandYouControl)
        effect = Effects.May(
            Effects.BecomeCreature(
                target = land,
                power = 3,
                toughness = 3,
                keywords = setOf(Keyword.HASTE),
                creatureTypes = setOf("Elemental"),
                duration = Duration.EndOfTurn,
            )
        )
        description = "Landfall — Whenever a land you control enters, you may have target land you " +
            "control become a 3/3 Elemental creature with haste until end of turn. It's still a land."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "107"
        artist = "David Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/9/2/92f385d1-a6c5-40b7-bc90-6f71b6f815e7.jpg?1783937907"

        ruling("2016-01-22", "A “land creature” is a permanent that’s both a land and a creature.")
        ruling(
            "2016-01-22",
            "You may target a land that’s already a creature. For example, if you target a land " +
                "that’s also a 0/0 creature and has three +1/+1 counters on it, the resulting land " +
                "creature will be 6/6.",
        )
    }
}
