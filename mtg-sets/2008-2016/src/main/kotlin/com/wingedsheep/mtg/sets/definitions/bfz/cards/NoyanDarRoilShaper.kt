package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Noyan Dar, Roil Shaper — Battle for Zendikar #216
 * {3}{W}{U} · Legendary Creature — Merfolk Ally · 4/4
 *
 * Whenever you cast an instant or sorcery spell, you may put three +1/+1 counters on target land you
 * control. If you do, that land becomes a 0/0 Elemental creature with haste that's still a land.
 *
 * Same shape as Cyclone Sire, on a cast trigger: the land is targeted when the trigger goes on the
 * stack, the "may" is answered on resolution, and declining skips both halves ("If you do"). The
 * trigger resolves before the spell that caused it. The animation is permanent and keeps the land's
 * types and abilities. Not awaken — no keyword is granted.
 */
val NoyanDarRoilShaper = card("Noyan Dar, Roil Shaper") {
    manaCost = "{3}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Merfolk Ally"
    power = 4
    toughness = 4
    oracleText = "Whenever you cast an instant or sorcery spell, you may put three +1/+1 counters on " +
        "target land you control. If you do, that land becomes a 0/0 Elemental creature with haste " +
        "that's still a land."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        val land = target(TargetFilter(GameObjectFilter.Land.youControl()))
        effect = Effects.May(
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 3, land) then
                Effects.BecomeCreature(
                    target = land,
                    power = 0,
                    toughness = 0,
                    keywords = setOf(Keyword.HASTE),
                    creatureTypes = setOf("Elemental"),
                    duration = Duration.Permanent,
                )
        )
        description = "Whenever you cast an instant or sorcery spell, you may put three +1/+1 counters on " +
            "target land you control. If you do, that land becomes a 0/0 Elemental creature with haste " +
            "that's still a land."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "216"
        artist = "Karl Kopinski"
        flavorText = "After many years as a lullmage, Noyan Dar turned his talents from allaying Zendikar's " +
            "fury to the considerably easier task of rousing it."
        imageUri = "https://cards.scryfall.io/normal/front/0/1/018a66fc-e7aa-42df-a622-50743cb20183.jpg?1783938179"
        ruling(
            "2015-08-25",
            "Although Noyan Dar's ability is very similar to awaken, it is not the same ability. Any card " +
                "that refers to a \"card with awaken\" doesn't include Noyan Dar."
        )
        ruling(
            "2015-08-25",
            "The ability will resolve before the instant or sorcery spell that caused it to trigger."
        )
    }
}
