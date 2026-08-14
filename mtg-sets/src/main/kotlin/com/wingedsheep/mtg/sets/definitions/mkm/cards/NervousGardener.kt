package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Nervous Gardener — Murders at Karlov Manor #169
 * {1}{G} · Creature — Dryad · 2/2
 *
 * Disguise {G}
 * When this creature is turned face up, search your library for a land card with a basic land type,
 * reveal it, put it into your hand, then shuffle.
 *
 * The whole card is the disguise line: hard-cast for {1}{G} it's a vanilla 2/2, because turning face
 * up is a special action and not entering the battlefield (CR 701.34), so nothing about a normal cast
 * ever reaches the trigger. The intended curve is {3} face down on turn three, then {G} to flip and
 * fetch — cheapest flip cost in the set.
 *
 * "A land card **with a basic land type**" is broader than a basic land card: it's any land whose
 * subtypes include one of the five basic land types (CR 305.6), so shocklands, triomes, and Karlov
 * Manor's own surveil duals all qualify — this is a subtype test, not a supertype test, and using
 * [Filters.BasicLand] here would silently narrow it to actual basics. Composed off
 * [Subtype.ALL_BASIC_LAND_TYPES] so it tracks the canonical list rather than a second hand-written one.
 *
 * The search is `ChooseUpTo(1)`, which is how the rules read it anyway: you're never forced to find
 * (CR 701.19c), and choosing nothing is the legal "fail to find".
 */
val NervousGardener = card("Nervous Gardener") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Dryad"
    oracleText = "Disguise {G} (You may cast this card face down for {3} as a 2/2 creature with ward " +
        "{2}. Turn it face up any time for its disguise cost.)\n" +
        "When this creature is turned face up, search your library for a land card with a basic land " +
        "type, reveal it, put it into your hand, then shuffle."
    power = 2
    toughness = 2

    disguise = "{G}"

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        effect = Patterns.Library.searchLibrary(
            filter = Filters.Land.withAnyOfSubtypes(Subtype.ALL_BASIC_LAND_TYPES.map { Subtype(it) }),
            destination = SearchDestination.HAND,
            reveal = true
        )
        description = "When this creature is turned face up, search your library for a land card with " +
            "a basic land type, reveal it, put it into your hand, then shuffle."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "169"
        artist = "Borja Pindado"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/93b747c7-b342-47f8-a190-16c393b20607.jpg?1783912862"

        ruling(
            "2024-02-02",
            "Any time you have priority, you may turn the face-down creature face up by revealing " +
                "what its disguise cost is and paying that cost. This is a special action. It " +
                "doesn't use the stack and can't be responded to."
        )
        ruling(
            "2024-02-02",
            "Because the permanent is on the battlefield both before and after it's turned face up, " +
                "turning a permanent face up doesn't cause any enters-the-battlefield abilities to trigger."
        )
    }
}
