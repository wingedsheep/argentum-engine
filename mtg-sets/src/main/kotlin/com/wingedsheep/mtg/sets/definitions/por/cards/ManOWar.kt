// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature


/**
 * Man-o'-War
 * {2}{U}
 * Creature — Jellyfish
 * 2/2
 * When this creature enters, return target creature to its owner's hand.
 */
val ManoWar = card("Man-o'-War") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Jellyfish"
    power = 2
    toughness = 2
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target", TargetCreature(filter = TargetFilter.Creature))
        effect = MoveToZoneEffect(t, Zone.HAND)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "59"
        artist = "Una Fricker"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e835b618-83c1-46e2-b8bd-aec56f58ccfc.jpg"
    }
}
