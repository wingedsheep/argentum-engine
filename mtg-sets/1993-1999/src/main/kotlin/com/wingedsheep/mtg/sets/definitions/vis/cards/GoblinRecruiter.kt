package com.wingedsheep.mtg.sets.definitions.vis.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect

/**
 * Goblin Recruiter
 * {1}{R}
 * Creature — Goblin
 * 1/1
 *
 * When this creature enters, search your library for any number of Goblin cards, reveal them, then
 * shuffle and put those cards on top in any order.
 *
 * Scouting Trek's Gather → Select → Move pipeline over Goblin cards (any card with the Goblin
 * subtype, per the ruling), with the printed "in any order" as [CardOrder.ControllerChooses].
 */
val GoblinRecruiter = card("Goblin Recruiter") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    oracleText = "When this creature enters, search your library for any number of Goblin cards, " +
        "reveal them, then shuffle and put those cards on top in any order."
    power = 1
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(Zone.LIBRARY, filter = GameObjectFilter.Any.withSubtype(Subtype.GOBLIN)),
                search = true
            )
            val found = chooseAnyNumber(
                from = searchable,
                prompt = "Search your library for any number of Goblin cards"
            )
            reveal(found)
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
            toLibraryTop(found, order = CardOrder.ControllerChooses)
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "80"
        artist = "Scott Kirschner"
        flavorText = "\"Next!\""
        imageUri = "https://cards.scryfall.io/normal/front/6/e/6ee791d5-1d48-40e8-b65f-b6aa889f3467.jpg?1783946989"
        ruling(
            "2008-04-01",
            "You can search for any card with the creature type Goblin, including creature or kindred " +
                "cards. Older cards of type \"Summon Goblin\" and \"Summon Goblins\" also count. You can't " +
                "search for cards that have Goblin in the name but don't have the creature type Goblin."
        )
    }
}
