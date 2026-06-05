// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature


/**
 * Serpent Assassin
 * {3}{B}{B}
 * Creature — Snake Assassin
 * 2/2
 * When this creature enters, you may destroy target nonblack creature.
 */
val SerpentAssassin = card("Serpent Assassin") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Snake Assassin"
    power = 2
    toughness = 2
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target", TargetCreature(filter = TargetFilter.Creature.notColor(Color.BLACK)))
        effect = MayEffect(MoveToZoneEffect(t, Zone.GRAVEYARD, byDestruction = true))
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "108"
        artist = "Roger Raupp"
        imageUri = "https://cards.scryfall.io/normal/front/1/0/1018f6ff-5eaa-4fe1-ba20-544df799f5b2.jpg"
    }
}
