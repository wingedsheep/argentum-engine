package com.wingedsheep.mtg.sets.definitions.dka.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.SelectionMode

/**
 * Tracker's Instincts
 * {1}{G}
 * Sorcery
 * Reveal the top four cards of your library. Put a creature card from among them into your hand
 * and the rest into your graveyard.
 * Flashback {2}{U}
 */
val TrackersInstincts = card("Tracker's Instincts") {
    manaCost = "{1}{G}"
    colorIdentity = "UG"
    typeLine = "Sorcery"
    oracleText = "Reveal the top four cards of your library. Put a creature card from among them into your hand and the rest into your graveyard.\nFlashback {2}{U} (You may cast this card from your graveyard for its flashback cost. Then exile it.)"

    spell {
        effect = Patterns.Library.lookAtTopAndTakeMatching(
            count = DynamicAmounts.fixed(4),
            filter = GameObjectFilter.Creature,
            prompt = "Put a creature card from among them into your hand",
            selection = SelectionMode.ChooseExactly(DynamicAmounts.fixed(1)),
            revealed = true,
            restDestination = CardDestination.ToZone(Zone.GRAVEYARD),
            restOrder = CardOrder.Preserve,
        )
    }

    keywordAbility(KeywordAbility.flashback("{2}{U}"))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "128"
        artist = "Jung Park"
        imageUri = "https://cards.scryfall.io/normal/front/5/9/59960387-3adf-4b9a-b0e6-c441579f7388.jpg"
    }
}
