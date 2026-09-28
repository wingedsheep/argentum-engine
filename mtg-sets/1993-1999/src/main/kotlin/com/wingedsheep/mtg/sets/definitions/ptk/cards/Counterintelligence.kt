package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Counterintelligence
 * {2}{U}{U}
 * Sorcery
 *
 * Return one or two target creatures to their owners' hands.
 */
val Counterintelligence = card("Counterintelligence") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Return one or two target creatures to their owners' hands."

    spell {
        targets(TargetFilter.Creature, count = 2, minCount = 1)
        effect = Effects.ForEachTarget(Effects.Move(EffectTarget.ContextTarget(0), Zone.HAND))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "41"
        artist = "Wang Feng"
        flavorText = "Before the battle of Red Cliffs, a supposedly sleeping Zhou Yu allowed his old friend, a Wei advisor, to steal a planted letter forged as if from Wei's two best admirals."
        imageUri = "https://cards.scryfall.io/normal/front/e/a/eafbeafb-ef84-4a8d-9ca8-ca305b1feeea.jpg?1783946124"
    }
}
