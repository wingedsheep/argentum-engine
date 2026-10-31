package com.wingedsheep.mtg.sets.definitions.m12.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tectonic Rift
 * {3}{R}
 * Sorcery
 * Destroy target land. Creatures without flying can't block this turn.
 *
 * The block restriction is a dynamic group restriction (it doesn't change characteristics), so
 * creatures without flying that enter later this turn can't block, and a creature that gains
 * flying stops being affected. If the land target is illegal on resolution the whole spell
 * fizzles; an indestructible land still leaves the restriction in place.
 */
val TectonicRift = card("Tectonic Rift") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Destroy target land. Creatures without flying can't block this turn."

    spell {
        val land = target(TargetFilter.Land)
        effect = Effects.Destroy(land) then Effects.CantBlockGroup(
            GroupFilter(GameObjectFilter.Creature.withoutKeyword(Keyword.FLYING))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "157"
        artist = "John Avon"
        flavorText = "\"You will kneel before me, even if I have to split the earth under your feet!\"\n—Ash Kronor, Keldon warlord"
        imageUri = "https://cards.scryfall.io/normal/front/e/9/e9838784-8c6d-4e64-bc34-e21efde99093.jpg?1783941064"
        ruling(
            "2019-07-12",
            "If the target land is an illegal target by the time Tectonic Rift tries to resolve, the spell doesn't resolve. Creatures without flying will be able to block as normal. If the target is legal but not destroyed (most likely because it has indestructible), creatures without flying will be unable to block."
        )
        ruling(
            "2019-07-12",
            "Because it doesn't modify the characteristics of creatures, Tectonic Rift can affect creatures that enter the battlefield or lose flying after the spell resolves, and it will stop affecting creatures that gain flying."
        )
    }
}
