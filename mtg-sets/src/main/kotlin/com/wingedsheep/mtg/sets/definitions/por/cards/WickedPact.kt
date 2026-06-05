// === GENERATED DRAFT — do NOT merge as-is. ===
// Source: mtgish IR via the coverage bridge (predictive, approximate).
// Before use: (1) compile, (2) write & pass a scenario test, (3) review the rules text.
// Then move into the set's cards/ package (auto-registers via classpath scan).

package com.wingedsheep.mtg.sets.definitions.por.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.LoseLifeEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature


/**
 * Wicked Pact
 * {1}{B}{B}
 * Sorcery
 * Destroy two target nonblack creatures. You lose 5 life.
 */
val WickedPact = card("Wicked Pact") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    spell {
        val t = target("target", TargetCreature(count = 2, filter = TargetFilter.Creature.notColor(Color.BLACK)))
        effect = CompositeEffect(
        listOf(
            ForEachTargetEffect(listOf(MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.GRAVEYARD, byDestruction = true))),
            LoseLifeEffect(5, EffectTarget.Controller)
        )
    )
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "117"
        artist = "Adam Rex"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e4d7c251-cb65-4ffc-8bf0-5e9692004a87.jpg"
    }
}
