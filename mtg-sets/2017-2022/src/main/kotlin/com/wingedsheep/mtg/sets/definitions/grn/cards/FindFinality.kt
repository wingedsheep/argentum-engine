package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Find // Finality (GRN 225) — split-layout spell (CR 709).
 *
 * Find {B/G}{B/G} — Sorcery
 *   Return up to two target creature cards from your graveyard to your hand.
 *
 * Finality {4}{B}{G} — Sorcery
 *   You may put two +1/+1 counters on a creature you control. Then all creatures get -4/-4
 *   until end of turn.
 *
 * Finality's counters don't target: the creature is chosen on resolution, inside the "may",
 * before the global -4/-4 (which only touches creatures on the battlefield at that moment).
 */
val FindFinality = card("Find // Finality") {
    layout = CardLayout.SPLIT
    colorIdentity = "BG"

    face("Find") {
        manaCost = "{B/G}{B/G}"
        typeLine = "Sorcery"
        oracleText = "Return up to two target creature cards from your graveyard to your hand."

        spell {
            targets(TargetFilter.CreatureInYourGraveyard, count = 2, optional = true)
            effect = Effects.ForEachTarget(Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND))
        }
    }

    face("Finality") {
        manaCost = "{4}{B}{G}"
        typeLine = "Sorcery"
        oracleText = "You may put two +1/+1 counters on a creature you control. " +
            "Then all creatures get -4/-4 until end of turn."

        spell {
            effect = Effects.May(
                Effects.Pipeline {
                    val chosen = selectTarget(
                        TargetObject(filter = TargetFilter.CreatureYouControl),
                        nonTargeting = true,
                        prompt = "Choose a creature you control to get two +1/+1 counters"
                    )
                    run(Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, chosen.asTarget))
                },
                prompt = "Put two +1/+1 counters on a creature you control?"
            ) then Patterns.Group.modifyStatsForAll(-4, -4, GroupFilter.AllCreatures)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "225"
        artist = "Tomasz Jedruszek"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e4e9e35-6cbc-4997-beff-d1a22d87545e.jpg?1783934112"
        ruling("2018-10-05", "Finality doesn't target the creature to receive +1/+1 counters. You can cast it even if you control no creatures.")
        ruling("2018-10-05", "Finality affects only creatures on the battlefield at the time it resolves. Creatures that enter the battlefield later in the turn won't get -4/-4.")
    }
}
