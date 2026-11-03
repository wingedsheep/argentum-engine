package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersUntapped
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Warp Speed
 * {2}{G}
 * Enchantment
 *
 * Lands you control enter untapped.
 * When this enchantment enters, search your library for a basic land card or Planet card, reveal
 * it, put it into your hand, then shuffle.
 * Strange new worlds — {7}{U}{U}, Sacrifice this enchantment: Draw two cards. This ability costs
 * {X} less to activate, where X is the number of differently named lands you control.
 *
 * "Lands you control enter untapped" is the static [EntersUntapped] replacement, as on Spelunking.
 * The self-reduction is the ability's `genericCostReduction`, as on Astute Terraformer; it only
 * reduces the generic {7}. "Strange new worlds" is an ability word with no rules meaning.
 */
val WarpSpeed = card("Warp Speed") {
    manaCost = "{2}{G}"
    typeLine = "Enchantment"
    colorIdentity = "GU"
    oracleText = "Lands you control enter untapped.\n" +
        "When this enchantment enters, search your library for a basic land card or Planet card, reveal it, " +
        "put it into your hand, then shuffle.\n" +
        "Strange new worlds — {7}{U}{U}, Sacrifice this enchantment: Draw two cards. This ability costs {X} " +
        "less to activate, where X is the number of differently named lands you control."

    replacementEffect(
        EntersUntapped(
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Land.youControl(),
                to = Zone.BATTLEFIELD,
            )
        )
    )

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.BasicLand or GameObjectFilter.Land.withSubtype("Planet"),
            destination = SearchDestination.HAND,
            reveal = true
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{7}{U}{U}"), Costs.SacrificeSelf)
        effect = Effects.DrawCards(2)
        genericCostReduction = DynamicAmounts.differentlyNamedLandsYouControl()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "223"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fdb5b940-9f62-47ad-8e80-5996daad0c9b.jpg?1793188026"
    }
}
