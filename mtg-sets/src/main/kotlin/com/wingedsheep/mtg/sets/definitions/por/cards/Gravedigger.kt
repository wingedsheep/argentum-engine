// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject


/**
 * Gravedigger
 * {3}{B}
 * Creature — Zombie
 * 2/2
 * When this creature enters, you may return target creature card from your graveyard to your hand.
 */
val Gravedigger = card("Gravedigger") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 2
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target", TargetObject(filter = TargetFilter.CreatureInYourGraveyard))
        effect = MayEffect(MoveToZoneEffect(t, Zone.HAND))
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "95"
        artist = "Scott M. Fischer"
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b979d70e-d514-420f-886c-f60e2bb1861f.jpg"
    }
}
