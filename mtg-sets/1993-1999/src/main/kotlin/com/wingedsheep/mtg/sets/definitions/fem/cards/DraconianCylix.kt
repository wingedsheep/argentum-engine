package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.RegenerateEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Draconian Cylix
 * {3}
 * Artifact
 * {2}, {T}, Discard a card at random: Regenerate target creature.
 */
val DraconianCylix = card("Draconian Cylix") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "{2}, {T}, Discard a card at random: Regenerate target creature."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.DiscardAtRandom(1))
        val t = target("target creature", TargetCreature(filter = TargetFilter.Creature))
        effect = RegenerateEffect(t)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "86"
        artist = "Edward P. Beard, Jr."
        flavorText = "\"There is no gain without sacrifice.\"\n—Icatian proverb"
        imageUri = "https://cards.scryfall.io/normal/front/a/4/a419c9e3-5615-44f9-9256-94a3022bb69f.jpg?1783947881"
    }
}
