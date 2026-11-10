package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Elemental Uprising — Oath of the Gatewatch #130 (canonical printing)
 * {1}{G} · Instant
 *
 * Target land you control becomes a 4/4 Elemental creature with haste until end of turn. It's
 * still a land. It must be blocked this turn if able.
 *
 * Disturbed Slumber's shape: [Effects.BecomeCreature] keeps the LAND type and its land subtypes,
 * then a floating must-be-blocked requirement (one blocker suffices — `allCreatures = false`).
 */
val ElementalUprising = card("Elemental Uprising") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Target land you control becomes a 4/4 Elemental creature with haste until end of turn. " +
        "It's still a land. It must be blocked this turn if able."

    spell {
        val land = target(TargetFilter(GameObjectFilter.Land.youControl()))
        effect = Effects.BecomeCreature(
            target = land,
            power = 4,
            toughness = 4,
            keywords = setOf(Keyword.HASTE),
            creatureTypes = setOf("Elemental"),
            duration = Duration.EndOfTurn,
        ) then
            Effects.MustBeBlocked(land, allCreatures = false)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "130"
        artist = "Svetlin Velinov"
        flavorText = "\"My heart and Zendikar's beat as one. Together we will endure.\"\n—Nissa Revane"
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e194b283-a7d2-4c0a-9ee2-8689a8e911b9.jpg?1783937902"
        ruling("2016-01-22", "If the resulting creature attacks, the defending player must assign at least one blocker to it during the declare blockers step if that player controls any creatures that could block it.")
        ruling("2016-01-22", "You may target a land that's already a creature. For example, if you target a land that's also a 0/0 creature and has three +1/+1 counters on it, the resulting land creature will be 7/7.")
    }
}
