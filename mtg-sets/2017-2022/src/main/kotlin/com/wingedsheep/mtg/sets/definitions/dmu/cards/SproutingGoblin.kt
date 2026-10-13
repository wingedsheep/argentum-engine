package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Sprouting Goblin — Dominaria United #145
 *
 * "A land card with a basic land type" is a subtype test (any of the five basic land types), not
 * the basic supertype — nonbasic duals with a basic land type qualify, per the 2022-09-09 ruling.
 */
val SproutingGoblin = card("Sprouting Goblin") {
    manaCost = "{1}{R}"
    colorIdentity = "RG"
    typeLine = "Creature — Goblin Druid"
    power = 2
    toughness = 2
    oracleText = "Kicker {G} (You may pay an additional {G} as you cast this spell.)\n" +
        "When this creature enters, if it was kicked, search your library for a land card with a basic " +
        "land type, reveal it, put it into your hand, then shuffle.\n" +
        "{R}, {T}, Sacrifice a land: Draw a card."

    keywordAbility(KeywordAbility.kicker("{G}"))

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.WasKicked
        effect = Patterns.Library.searchLibrary(
            filter = Filters.Land.withAnyOfSubtypes(Subtype.ALL_BASIC_LAND_TYPES.map { Subtype(it) }),
            destination = SearchDestination.HAND,
            reveal = true
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}"), Costs.Tap, Costs.Sacrifice(GameObjectFilter.Land))
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "145"
        artist = "Uriah Voth"
        imageUri = "https://cards.scryfall.io/normal/front/6/0/602f5370-d096-4765-b612-16bcec3aafa7.jpg?1783921310"

        ruling(
            "2022-09-09",
            "You may search for any land card with one of the basic land types, even if that land isn't " +
                "a basic land. The basic land types are Plains, Island, Swamp, Mountain, and Forest."
        )
    }
}
