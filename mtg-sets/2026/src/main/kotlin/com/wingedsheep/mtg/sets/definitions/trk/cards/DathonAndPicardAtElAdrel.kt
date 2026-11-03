package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dathon and Picard at El-Adrel
 * {2}{G}
 * Instant
 *
 * Choose one —
 * • Temba, His Arms Wide — Target creature gets +3/+3 and gains trample until end of turn.
 * • Shaka, When the Walls Fell — Each opponent sacrifices a nontoken artifact or enchantment of
 *   their choice.
 */
val DathonAndPicardAtElAdrel = card("Dathon and Picard at El-Adrel") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Temba, His Arms Wide — Target creature gets +3/+3 and gains trample until end of turn.\n" +
        "• Shaka, When the Walls Fell — Each opponent sacrifices a nontoken artifact or enchantment of their choice."

    spell {
        modal(chooseCount = 1) {
            mode("Temba, His Arms Wide — Target creature gets +3/+3 and gains trample until end of turn") {
                val creature = target(TargetFilter(GameObjectFilter.Creature))
                effect = Effects.ModifyStats(3, 3, creature) then
                    Effects.GrantKeyword(Keyword.TRAMPLE, creature)
            }

            mode("Shaka, When the Walls Fell — Each opponent sacrifices a nontoken artifact or enchantment of their choice") {
                effect = Effects.Sacrifice(
                    filter = GameObjectFilter.ArtifactOrEnchantment.nontoken(),
                    target = EffectTarget.PlayerRef(Player.EachOpponent)
                )
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "192"
        artist = "Bartek Fedyczak"
        imageUri = "https://cards.scryfall.io/normal/front/9/4/94e9d400-f2bb-46e7-8d41-6038ebdc6c8b.jpg?1785981571"
    }
}
