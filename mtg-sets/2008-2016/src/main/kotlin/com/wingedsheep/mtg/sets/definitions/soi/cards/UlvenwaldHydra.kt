package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Ulvenwald Hydra (Shadows over Innistrad #235)
 * {4}{G}{G}
 * Creature — Hydra
 * * / *
 *
 * Reach
 * Ulvenwald Hydra's power and toughness are each equal to the number of lands you control.
 * When this creature enters, you may search your library for a land card, put it onto the
 * battlefield tapped, then shuffle.
 */
val UlvenwaldHydra = card("Ulvenwald Hydra") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Hydra"
    oracleText = "Reach\n" +
        "Ulvenwald Hydra's power and toughness are each equal to the number of lands you control.\n" +
        "When this creature enters, you may search your library for a land card, put it onto the " +
        "battlefield tapped, then shuffle."

    dynamicStats(DynamicAmounts.landsYouControl())

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        optional = true
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Land,
            destination = SearchDestination.BATTLEFIELD,
            entersTapped = true
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "235"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8a5d48df-7d21-462c-b721-b769a785bb00.jpg"
    }
}
