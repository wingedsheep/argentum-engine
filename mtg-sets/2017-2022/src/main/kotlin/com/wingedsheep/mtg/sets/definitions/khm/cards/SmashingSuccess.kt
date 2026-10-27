package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Smashing Success
 * {3}{R}
 * Instant
 * Destroy target artifact or land. If an artifact is destroyed this way, create a Treasure token.
 *
 * "An artifact" is the target's characteristics as it last existed on the battlefield, so the
 * artifact check is taken from projected state *before* the destroy (a land made into an artifact
 * by a continuous effect counts; an artifact land counts). "Destroyed this way" is read off what the
 * destroy step actually moved: an indestructible or regenerated artifact yields no Treasure.
 */
val SmashingSuccess = card("Smashing Success") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Destroy target artifact or land. If an artifact is destroyed this way, create a " +
        "Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any color.\")"

    spell {
        target(TargetFilter.ArtifactOrLand)
        effect = Effects.Pipeline {
            val targeted = gather(CardSource.ChosenTargets)
            val wasArtifact = filter(targeted, GameObjectFilter.Artifact)
            val destroyed = moveTracked(
                targeted,
                CardDestination.ToZone(Zone.GRAVEYARD),
                moveType = MoveType.Destroy
            )
            ifNotEmpty(destroyed) {
                ifNotEmpty(wasArtifact) {
                    run(Effects.CreateTreasure(1))
                }
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "151"
        artist = "Slawomir Maniak"
        flavorText = "\"Who said there's no profit in destruction?\"\n—Magda, brazen outlaw"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c22c7383-ba4d-4b3c-91d6-2d6528e19825.jpg?1783928222"
    }
}
