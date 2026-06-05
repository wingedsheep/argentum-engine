// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.DrawCardsEffect
import com.wingedsheep.sdk.scripting.effects.RevealHandEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.values.DynamicAmount


/**
 * Baleful Stare
 * {2}{U}
 * Sorcery
 * Target opponent reveals their hand. You draw a card for each Mountain and red card in it.
 */
val BalefulStare = card("Baleful Stare") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetOpponent())
        effect = CompositeEffect(
        listOf(
            RevealHandEffect(t),
            DrawCardsEffect(DynamicAmount.Count(Player.TargetOpponent, Zone.HAND, (GameObjectFilter.Land.withSubtype("Mountain") or GameObjectFilter.Any.withColor(Color.RED))))
        )
    )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "42"
        artist = "John Coulthart"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49fb46c8-30ae-4457-a726-6fe1ddd183d5.jpg"
    }
}
