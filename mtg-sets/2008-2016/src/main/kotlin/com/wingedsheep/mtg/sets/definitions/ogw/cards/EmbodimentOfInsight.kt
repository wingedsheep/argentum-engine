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
 * Embodiment of Insight — Oath of the Gatewatch #131
 * {4}{G} · Creature — Elemental · 4/4
 *
 * Vigilance
 * Land creatures you control have vigilance.
 * Landfall — Whenever a land you control enters, you may have target land you control become a
 * 3/3 Elemental creature with haste until end of turn. It's still a land.
 *
 * The target is chosen when the trigger goes on the stack; the "you may" is asked on resolution.
 * [Effects.BecomeCreature] adds the creature type and Elemental subtype on top of the land's own
 * types, so it keeps its land types and mana abilities. The vigilance grant reads projected state, so
 * the land this animates (or any other land creature) has vigilance while it's a creature.
 */
val EmbodimentOfInsight = card("Embodiment of Insight") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    power = 4
    toughness = 4
    oracleText = "Vigilance\n" +
        "Land creatures you control have vigilance.\n" +
        "Landfall — Whenever a land you control enters, you may have target land you control become " +
        "a 3/3 Elemental creature with haste until end of turn. It's still a land."

    keywords(Keyword.VIGILANCE)

    staticAbility {
        ability = GrantKeyword(
            Keyword.VIGILANCE,
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
        collectorNumber = "131"
        artist = "Marco Nelor"
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b034a95d-4f51-4f04-94c2-dad073ea0f35.jpg?1783937902"

        ruling("2016-01-22", "A “land creature” is a permanent that’s both a land and a creature.")
        ruling(
            "2016-01-22",
            "You may target a land that’s already a creature. For example, if you target a land " +
                "that’s also a 0/0 creature and has three +1/+1 counters on it, the resulting land " +
                "creature will be 6/6.",
        )
    }
}
