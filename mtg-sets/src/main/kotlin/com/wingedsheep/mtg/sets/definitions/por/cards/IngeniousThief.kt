// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.LookAtTargetHandEffect
import com.wingedsheep.sdk.scripting.targets.TargetPlayer


/**
 * Ingenious Thief
 * {1}{U}
 * Creature — Human Rogue
 * 1/1
 * Flying
 * When this creature enters, look at target player's hand.
 */
val IngeniousThief = card("Ingenious Thief") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Rogue"
    power = 1
    toughness = 1
    keywords(Keyword.FLYING)
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target", TargetPlayer())
        effect = LookAtTargetHandEffect(t)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "58"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/b/e/be341805-b4de-456e-8b46-4ee5fdbca7e0.jpg"
    }
}
