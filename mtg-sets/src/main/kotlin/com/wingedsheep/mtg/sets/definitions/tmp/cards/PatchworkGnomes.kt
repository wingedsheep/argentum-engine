package com.wingedsheep.mtg.sets.definitions.tmp.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Patchwork Gnomes
 * {3}
 * Artifact Creature — Gnome
 * 2/1
 *
 * Discard a card: Regenerate this creature.
 */
val PatchworkGnomes = card("Patchwork Gnomes") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Gnome"
    power = 2
    toughness = 1
    oracleText = "Discard a card: Regenerate this creature."

    activatedAbility {
        cost = Costs.DiscardCard
        effect = RegenerateEffect(EffectTarget.Self)
        description = "Discard a card: Regenerate this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "299"
        artist = "Mike Raabe"
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bdaa9ac4-b742-4a24-a316-97538adfd361.jpg?1562056382"
    }
}
