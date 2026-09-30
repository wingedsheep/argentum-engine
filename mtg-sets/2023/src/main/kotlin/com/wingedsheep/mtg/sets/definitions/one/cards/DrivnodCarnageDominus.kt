package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalDeathTriggers
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Drivnod, Carnage Dominus
 * {3}{B}{B}
 * Legendary Creature — Phyrexian Horror
 * 8/3
 *
 * If a creature dying causes a triggered ability of a permanent you control to trigger, that
 * ability triggers an additional time.
 * {B/P}{B/P}, Exile three creature cards from your graveyard: Put an indestructible counter on Drivnod.
 */
val DrivnodCarnageDominus = card("Drivnod, Carnage Dominus") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Phyrexian Horror"
    power = 8
    toughness = 3
    oracleText = "If a creature dying causes a triggered ability of a permanent you control to trigger, " +
        "that ability triggers an additional time.\n" +
        "{B/P}{B/P}, Exile three creature cards from your graveyard: Put an indestructible counter on " +
        "Drivnod. ({B/P} can be paid with either {B} or 2 life.)"

    staticAbility {
        ability = AdditionalDeathTriggers(permanentsYouControl = GameObjectFilter.Any)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{B/P}{B/P}"),
            Costs.ExileFromGraveyard(3, GameObjectFilter.Creature)
        )
        effect = Effects.AddCounters(CounterType.INDESTRUCTIBLE, 1, EffectTarget.Self)
        description = "{B/P}{B/P}, Exile three creature cards from your graveyard: Put an indestructible " +
            "counter on Drivnod."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "90"
        artist = "Bogdan Rezunenko"
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6bc26bca-c4d8-4e8b-a96a-9529fc2daed7.jpg?1783918048"
    }
}
