package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Oswald Fiddlebender — Adventures in the Forgotten Realms #28 (canonical printing)
 * {1}{W} · Legendary Creature — Gnome Artificer · 2/2
 *
 * The sacrificed artifact is a cost, so its mana value is read from the cost-payment snapshot
 * ([EffectTarget.SacrificedAsCost], last-known information — a sacrificed token counts as 0)
 * and fed into an exact-equality mana-value filter for the search.
 */
val OswaldFiddlebender = card("Oswald Fiddlebender") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Gnome Artificer"
    power = 2
    toughness = 2
    oracleText = "Magical Tinkering — {W}, {T}, Sacrifice an artifact: Search your library for an artifact " +
        "card with mana value equal to 1 plus the sacrificed artifact's mana value, put it onto the " +
        "battlefield, then shuffle. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{W}"), Costs.Tap, Costs.Sacrifice(GameObjectFilter.Artifact))
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Artifact.manaValueEqualsDynamic(
                DynamicAmounts.manaValueOf(EffectTarget.SacrificedAsCost(0)) + 1
            ),
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            shuffleAfter = true,
        )
        timing = TimingRule.SorcerySpeed
        description = "Magical Tinkering — {W}, {T}, Sacrifice an artifact: Search your library for an " +
            "artifact card with mana value equal to 1 plus the sacrificed artifact's mana value, put it " +
            "onto the battlefield, then shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "28"
        artist = "Steven Belledin"
        flavorText = "\"Can't see this airship crashing in the near future. Sturdy as a rock.\""
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bba1650f-eddf-49a9-820e-489cb8d5b6fa.jpg?1783926527"
    }
}
