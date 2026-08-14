package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Murderous Compulsion (Shadows over Innistrad #126)
 * {1}{B}
 * Sorcery
 *
 * Destroy target tapped creature.
 * Madness {1}{B}
 *
 * "Tapped" is part of the target requirement, not a resolution check: the creature must be tapped
 * when targeted and still be a legal target on resolution, so untapping it in response fizzles the
 * spell. Madness (CR 702.35) is what makes it playable on the opponent's turn — the cast happens
 * while the madness trigger resolves, so the sorcery timing restriction doesn't apply and it can
 * answer a creature that just attacked.
 */
val MurderousCompulsion = card("Murderous Compulsion") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Destroy target tapped creature.\n" +
        "Madness {1}{B} (If you discard this card, discard it into exile. When you do, cast it " +
        "for its madness cost or put it into your graveyard.)"

    spell {
        target = TargetCreature(filter = TargetFilter.TappedCreature)
        effect = Effects.Destroy(EffectTarget.ContextTarget(0))
    }

    madness("{1}{B}")

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "126"
        artist = "David Palumbo"
        flavorText = "\"Thank you. This blade becomes so hot in my hand, but your blood has quenched it.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/3/33b94db1-ac8c-4667-81d5-408df0f30879.jpg?1783937768"
    }
}
