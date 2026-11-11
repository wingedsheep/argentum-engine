package com.wingedsheep.mtg.sets.definitions.ogw.cards

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
 * Cyclone Sire — Oath of the Gatewatch #54 (canonical printing)
 * {4}{U} · Creature — Elemental · 3/4
 *
 * Flying
 * When this creature dies, you may put three +1/+1 counters on target land you control. If you do,
 * that land becomes a 0/0 Elemental creature with haste that's still a land.
 *
 * Same shape as Wall of Resurgence, on a dies trigger: the target is chosen when the trigger goes on
 * the stack, the "may" is answered on resolution, and declining skips both halves ("If you do").
 * The animation is permanent and keeps the land's types and abilities.
 */
val CycloneSire = card("Cyclone Sire") {
    manaCost = "{4}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Elemental"
    power = 3
    toughness = 4
    oracleText = "Flying\n" +
        "When this creature dies, you may put three +1/+1 counters on target land you control. " +
        "If you do, that land becomes a 0/0 Elemental creature with haste that's still a land."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dies()
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
        description = "When this creature dies, you may put three +1/+1 counters on target land you " +
            "control. If you do, that land becomes a 0/0 Elemental creature with haste that's still a land."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "54"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bff62270-57ed-4706-bedf-11f60ae2da43.jpg?1783937919"
        ruling(
            "2016-01-22",
            "If you target a land that's already a creature with Cyclone Sire's triggered ability, " +
                "that land creature's base power and toughness will become 0/0, overwriting its previous " +
                "base power and toughness. Other effects that modify its power and/or toughness (including " +
                "the new and any previously-existing +1/+1 counters) will continue to apply."
        )
    }
}
