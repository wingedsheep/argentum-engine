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
 * Withering Gaze
 * {2}{U}
 * Sorcery
 * Target opponent reveals their hand. You draw a card for each Forest and green card in it.
 */
val WitheringGaze = card("Withering Gaze") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetOpponent())
        effect = CompositeEffect(
        listOf(
            RevealHandEffect(t),
            DrawCardsEffect(DynamicAmount.Count(Player.TargetOpponent, Zone.HAND, (GameObjectFilter.Land.withSubtype("Forest") or GameObjectFilter.Any.withColor(Color.GREEN))))
        )
    )
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "78"
        artist = "Scott M. Fischer"
        imageUri = "https://cards.scryfall.io/normal/front/0/e/0e952a48-9e60-4fce-8423-7f0bafd29bb1.jpg"
    }
}
