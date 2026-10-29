package com.wingedsheep.mtg.sets.definitions.jud.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Balthor the Defiled
 * {2}{B}{B}
 * Legendary Creature — Zombie Dwarf
 * 2/2
 *
 * Minion creatures get +1/+1.
 * {B}{B}{B}, Exile Balthor: Each player returns all black and all red creature cards from their
 * graveyard to the battlefield.
 *
 * The reanimation is the Twilight's Call pipeline narrowed to black-or-red creature cards, each
 * returned under its owner's control. Balthor is exiled as a cost, so it can't return itself.
 */
val BalthorTheDefiled = card("Balthor the Defiled") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Zombie Dwarf"
    power = 2
    toughness = 2
    oracleText = "Minion creatures get +1/+1.\n" +
        "{B}{B}{B}, Exile Balthor: Each player returns all black and all red creature cards from " +
        "their graveyard to the battlefield."

    staticAbility {
        ability = ModifyStats(
            powerBonus = 1,
            toughnessBonus = 1,
            filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Minion"))
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}{B}{B}"), Costs.ExileSelf)
        effect = Effects.Pipeline {
            val returning = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.Each,
                    filter = GameObjectFilter.Creature.withAnyColor(Color.BLACK, Color.RED)
                )
            )
            move(returning, CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
        }
        description = "Each player returns all black and all red creature cards from their " +
            "graveyard to the battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Carl Critchlow"
        flavorText = "He remembers enough of his life to weep for what he has lost."
        imageUri = "https://cards.scryfall.io/normal/front/e/d/ed4cc273-adc3-4f46-9743-134b552d1d56.jpg?1783945124"
        ruling("2013-07-01", "You can only activate this ability if Balthor the Defiled is on the battlefield.")
    }
}
