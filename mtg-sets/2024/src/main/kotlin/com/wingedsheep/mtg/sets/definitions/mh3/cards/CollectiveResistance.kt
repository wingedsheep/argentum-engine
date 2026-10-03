package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Collective Resistance — Modern Horizons 3 #147.
 *
 * Escalate {G} rides `additionalManaCostPerExtraMode`: one mode costs {1}{G}, two cost {1}{G}{G},
 * all three cost {1}{G}{G}{G}.
 */
val CollectiveResistance = card("Collective Resistance") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Escalate {G} (Pay this cost for each mode chosen beyond the first.)\n" +
        "Choose one or more —\n" +
        "• Destroy target artifact.\n" +
        "• Destroy target enchantment.\n" +
        "• Target creature gains hexproof and indestructible until end of turn."

    spell {
        modal(
            chooseCount = 3,
            minChooseCount = 1,
            additionalManaCostPerExtraMode = "{G}",
        ) {
            mode("Destroy target artifact.") {
                val artifact = target(TargetFilter.Artifact)
                effect = Effects.Destroy(artifact)
            }
            mode("Destroy target enchantment.") {
                val enchantment = target(TargetFilter.Enchantment)
                effect = Effects.Destroy(enchantment)
            }
            mode("Target creature gains hexproof and indestructible until end of turn.") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.GrantKeyword(Keyword.HEXPROOF, creature) then
                    Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, creature)
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "147"
        artist = "Raoul Vitale"
        imageUri = "https://cards.scryfall.io/normal/front/f/2/f260bd08-68b6-44f4-ace9-e298cb13d82e.jpg?1783911263"
    }
}
