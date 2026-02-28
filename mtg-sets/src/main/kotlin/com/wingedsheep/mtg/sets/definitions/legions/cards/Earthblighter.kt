package com.wingedsheep.mtg.sets.definitions.legions.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect

/**
 * Earthblighter
 * {1}{B}
 * Creature — Human Cleric
 * 1/1
 * {2}{B}, {T}, Sacrifice a Goblin: Destroy target land.
 */
val Earthblighter = card("Earthblighter") {
    manaCost = "{1}{B}"
    typeLine = "Creature — Human Cleric"
    power = 1
    toughness = 1
    oracleText = "{2}{B}, {T}, Sacrifice a Goblin: Destroy target land."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{2}{B}"),
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Creature.withSubtype("Goblin"))
        )
        val t = target("target", Targets.Land)
        effect = MoveToZoneEffect(t, Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "68"
        artist = "Alex Horley-Orlandelli"
        flavorText = "A single dedicated mind can bring about the greatest destruction. That, or goblins—goblins work too."
        imageUri = "https://cards.scryfall.io/normal/front/8/3/830a4048-48ac-4856-9af9-5052ec146518.jpg?1562921544"
    }
}
