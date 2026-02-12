package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EffectTarget
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.MoveToZoneEffect
import com.wingedsheep.sdk.targeting.TargetCreature

/**
 * Essence Fracture
 * {3}{U}{U}
 * Sorcery
 * Return two target creatures to their owners' hands.
 * Cycling {2}{U}
 */
val EssenceFracture = card("Essence Fracture") {
    manaCost = "{3}{U}{U}"
    typeLine = "Sorcery"
    oracleText = "Return two target creatures to their owners' hands.\nCycling {2}{U}"

    spell {
        target = TargetCreature(count = 2)
        effect = MoveToZoneEffect(EffectTarget.ContextTarget(0), Zone.HAND) then
                MoveToZoneEffect(EffectTarget.ContextTarget(1), Zone.HAND)
    }

    keywordAbility(KeywordAbility.Cycling(ManaCost.parse("{2}{U}")))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "82"
        artist = "Wayne England"
        flavorText = "\"Shaping reality is simply a matter of knowing where to apply pressure.\" —Ixidor, reality sculptor"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df0b6c7a-0891-492d-8e07-6a198bf2ccc4.jpg"
    }
}
