package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Winged Portent
 * {1}{U}{U}
 * Instant
 * Cleave {4}{G}{U} (You may cast this spell for its cleave cost. If you do, remove the words in
 * square brackets.)
 * Draw a card for each creature you control [with flying].
 *
 * Cleave (CR 702.148) removes the bracketed words when its alternative cost is paid. The printed
 * (cheaper) cast draws only for your fliers (a fits-a-skies-deck payoff); paying the cleave cost
 * drops the "with flying" restriction and draws for every creature you control.
 *
 * No target — only the effect differs, and only in the count filter. The count is evaluated on
 * resolution (CR 608.2), so it reflects the creatures you control at that time. `Player.You`
 * already scopes the battlefield query to your permanents, so the base [effect] counts your
 * fliers and the [cleaveEffect] counts all your creatures.
 */
val WingedPortent = card("Winged Portent") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "GU"
    typeLine = "Instant"
    oracleText = "Cleave {4}{G}{U} (You may cast this spell for its cleave cost. If you do, " +
        "remove the words in square brackets.)\nDraw a card for each creature you control " +
        "[with flying]."

    keywordAbility(KeywordAbility.cleave("{4}{G}{U}"))

    spell {
        // Printed (brackets present): draw a card for each creature you control with flying.
        effect = Effects.DrawCards(
            DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature.withKeyword(Keyword.FLYING),
            ).count(),
        )

        // Cleaved (brackets removed): draw a card for each creature you control.
        cleaveEffect = Effects.DrawCards(DynamicAmounts.creaturesYouControl())
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "89"
        artist = "Nicholas Gregory"
        flavorText = "\"No, no, it's just a murder of crows, not an omen of murder . . . I think.\"\n—York, Kessig tracker"
        imageUri = "https://cards.scryfall.io/normal/front/3/4/3494a4fc-37e5-4095-a3bb-5cd9280f4c77.jpg?1783924876"
    }
}
