package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetPlayer
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Singularity Rupture
 * {3}{U}{B}{B}
 * Sorcery
 * Destroy all creatures, then any number of target players each mill half their library, rounded down.
 */
val SingularityRupture = card("Singularity Rupture") {
    manaCost = "{3}{U}{B}{B}"
    colorIdentity = "UB"
    typeLine = "Sorcery"
    oracleText = "Destroy all creatures, then any number of target players each mill half their library, rounded down."

    spell {
        target = TargetPlayer(unlimited = true)
        effect = Effects.Composite(listOf(
            Effects.DestroyAll(GameObjectFilter.Creature),
            ForEachTargetEffect(
                LibraryPatterns.mill(
                    DynamicAmount.Divide(
                        DynamicAmount.Count(Player.ContextPlayer(0), Zone.LIBRARY),
                        DynamicAmount.Fixed(2),
                        roundUp = false
                    ),
                    EffectTarget.ContextTarget(0)
                ).effects
            )
        ))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "228"
        artist = "Liiga Smilshkalne"
        flavorText = "\"Rejoice! For within sekhar, the final sacrifice, you will find passage to the Next Eternity.\"\n—Calnan, Monoist assassin"
        imageUri = "https://cards.scryfall.io/normal/front/a/3/a34012e3-ec7a-4713-a2c2-f8efff49e364.jpg?1752947492"
    }
}
