package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule

/**
 * Snow-Covered Wastes
 * Basic Snow Land
 * {T}: Add {C}.
 *
 * Written with the plain [card] DSL rather than the `basicLand` helper, for the same reason as
 * `ice/cards/SnowCoveredIsland.kt`: that helper hardcodes a "Basic Land — <type>" type line and
 * cannot carry the Snow supertype. The intrinsic mana ability is spelled out here to match what
 * `basicLand("Wastes")` would have generated.
 *
 * Wastes has no land subtype, so the type line is bare "Basic Snow Land" — it is not a Plains,
 * Island, Swamp, Mountain, or Forest, and nothing that searches for a basic land *type* finds it.
 * Modern Horizons 3 is its first printing; the two other MH3 art treatments (309, 439) are
 * `Printing` rows alongside this canonical definition.
 */
val SnowCoveredWastes = card("Snow-Covered Wastes") {
    manaCost = ""
    colorIdentity = ""
    typeLine = "Basic Snow Land"
    oracleText = "{T}: Add {C}."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddColorlessMana(1)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "229"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87870792-e429-4eba-8193-cdce5c7b6c55.jpg?1783911237"

        // `CardDiscovery` files every basic land into `MtgSet.basicLands`, and
        // `BoosterGenerator.getBasicLands` turns that list — filtered by `inBooster` — into the
        // basics a limited player may add to their deck freely. A basic *snow* land must not be
        // free (see the ruling below), so it opts out here. Packs reach snow basics only through
        // reprint rows in a set's card pool, never through `MtgSet.basicLands`.
        inBooster = false

        ruling(
            "2024-06-07",
            "Because Snow-Covered Wastes is a basic land, you can include as many of them as you " +
                "like in your Constructed decks."
        )
        ruling(
            "2024-06-07",
            "In Limited events (including Sealed Deck and Booster Draft), Snow-Covered Wastes must " +
                "be in your card pool to be included in your deck. You can't add Snow-Covered " +
                "Wastes to your card pool in the same way that you can add other basic lands."
        )
        ruling(
            "2024-06-07",
            "Wastes is not a land type. If something asks you to name a land type, you can't " +
                "choose Wastes."
        )
        ruling(
            "2024-06-07",
            "Snow is a supertype, not a card type. It has no rules meaning or function by itself, " +
                "but spells and abilities may refer to it."
        )
    }
}
