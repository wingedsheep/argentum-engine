package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Mabel's Mettle {1}{W}
 * Instant
 *
 * Target creature gets +2/+2 until end of turn. Up to one other target creature
 * gets +1/+1 until end of turn.
 */
val MabelsMettle = card("Mabel's Mettle") {
    manaCost = "{1}{W}"
    typeLine = "Instant"
    oracleText = "Target creature gets +2/+2 until end of turn. Up to one other target creature gets +1/+1 until end of turn."

    spell {
        val primary = target("target creature", Targets.Creature)
        val secondary = target("up to one other target creature", TargetCreature(optional = true))
        effect = Effects.ModifyStats(2, 2, primary)
            .then(Effects.ModifyStats(1, 1, secondary))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Johan Grenier"
        flavorText = "\"You stole a child from its nest,\" Mabel accused. \"The only calamity I see here is you.\""
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5cfcf83f-089c-4e35-855e-b61b98bb1cd8.jpg?1721425880"
    }
}
