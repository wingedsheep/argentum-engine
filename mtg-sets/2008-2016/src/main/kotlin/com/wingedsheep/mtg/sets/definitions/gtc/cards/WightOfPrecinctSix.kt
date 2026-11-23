package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantDynamicStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Wight of Precinct Six — Gatecrash #84 (earliest printing)
 * {1}{B} · Creature — Zombie · 1/1
 *
 * This creature gets +1/+1 for each creature card in your opponents' graveyards.
 *
 * Liliana's Elite's shape with the graveyard count widened to every opponent:
 * a [GrantDynamicStats] on [GroupFilter.source] whose bonus is
 * a creature-card [DynamicAmounts.count] over [Player.EachOpponent]'s graveyards. Being a static
 * ability, it applies only while the Wight is on the battlefield (per the ruling).
 */
val WightOfPrecinctSix = card("Wight of Precinct Six") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 1
    toughness = 1
    oracleText = "This creature gets +1/+1 for each creature card in your opponents' graveyards."

    staticAbility {
        val creatureCards = DynamicAmounts.count(Player.EachOpponent, Zone.GRAVEYARD, GameObjectFilter.Creature)
        ability = GrantDynamicStats(
            filter = GroupFilter.source(),
            powerBonus = creatureCards,
            toughnessBonus = creatureCards
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "84"
        artist = "Ryan Barger"
        flavorText = "Even the lost and undead need a protector."
        imageUri = "https://cards.scryfall.io/normal/front/b/0/b04644ba-5962-4e64-bc53-92941c5b6715.jpg?1783940126"
        ruling(
            "2017-11-17",
            "Wight of Precinct Six's ability applies only while it's on the battlefield. In all other zones, it's a 1/1 creature.",
        )
        ruling(
            "2017-11-17",
            "If Wight of Precinct Six is dealt lethal damage at the same time as a creature an opponent controls, they're destroyed at the same time. It won't get an additional +1/+1 from its ability in time to save it.",
        )
    }
}
