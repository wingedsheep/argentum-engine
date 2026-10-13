package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Woodcaller Automaton — The Brothers' War #292 (non-booster extra)
 * {10} · Artifact Creature — Construct · 8/8 · Rare
 *
 * Prototype {2}{G}{G} — 3/3
 * When this creature enters, if you cast it, untap target land you control. It becomes a Treefolk
 * creature with haste and base power and toughness equal to this creature's power and toughness.
 * It's still a land.
 *
 * "If you cast it" is the any-zone [Conditions.WasCast] intervening-if (CR 603.4). The animate has
 * no duration, so it is [Duration.Permanent]. Its P/T is read off the Automaton once, as the
 * trigger resolves ([Effects.BecomeCreature] stamps the evaluated amount); `sourcePower()` falls
 * back to last-known information if the Automaton already left (ruling 2022-10-14). "It's still a
 * land" is BecomeCreature's default — CREATURE and Treefolk are added, LAND and its subtypes kept.
 */
val WoodcallerAutomaton = card("Woodcaller Automaton") {
    manaCost = "{10}"
    colorIdentity = "G"
    typeLine = "Artifact Creature — Construct"
    power = 8
    toughness = 8
    oracleText = "Prototype {2}{G}{G} — 3/3 (You may cast this spell with different mana cost, color, and size. It keeps its abilities and types.)\n" +
        "When this creature enters, if you cast it, untap target land you control. It becomes a Treefolk creature with haste and base power and toughness equal to this creature's power and toughness. It's still a land."

    keywordAbility(KeywordAbility.prototype("{2}{G}{G}", 3, 3))

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasCast
        val land = target(TargetFilter(GameObjectFilter.Land.youControl()))
        effect = Effects.Untap(land) then
            Effects.BecomeCreature(
                target = land,
                power = DynamicAmounts.sourcePower(),
                toughness = DynamicAmounts.sourceToughness(),
                keywords = setOf(Keyword.HASTE),
                creatureTypes = setOf("Treefolk"),
                duration = Duration.Permanent,
            )
        description = "When this creature enters, if you cast it, untap target land you control. " +
            "It becomes a Treefolk creature with haste and base power and toughness equal to this " +
            "creature's power and toughness. It's still a land."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "292"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a0418156-bcba-4edc-8ea5-ce9ca9b979bd.jpg?1783919994"
        ruling(
            "2022-10-14",
            "Use Woodcaller Automaton's power and toughness at the time its triggered ability resolves to " +
                "determine the base power and toughness of the Treefolk creature land. If Woodcaller Automaton " +
                "is no longer on the battlefield, use the power and toughness it had the last time it was on " +
                "the battlefield."
        )
    }
}
