package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Star of Extinction
 * {5}{R}{R}
 * Sorcery
 * Destroy target land. Star of Extinction deals 20 damage to each creature and each planeswalker.
 *
 * Lay Waste's land destruction followed by Storm's Wrath's sweep at 20. The land is the spell's only
 * target, so an illegal land target fizzles the whole spell (no sweep), as the rules require.
 */
val StarOfExtinction = card("Star of Extinction") {
    manaCost = "{5}{R}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Destroy target land. Star of Extinction deals 20 damage to each creature and each planeswalker."

    spell {
        val land = target(TargetFilter.Land)
        effect = Effects.Destroy(land) then
            Patterns.Group.dealDamageToAll(
                amount = 20,
                filter = GroupFilter(GameObjectFilter.CreatureOrPlaneswalker),
            )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "161"
        artist = "Chris Rahn"
        flavorText = "\"The visions haunt me. I see the end of the world, brought on by the search for the Immortal Sun.\"\n—Shaper Pashona"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/021f57dc-80f3-4ede-99d5-4a44aade44e2.jpg?1783935738"
    }
}
