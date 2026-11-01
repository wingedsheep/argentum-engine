package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sagittars' Volley — Ravnica Allegiance #138
 * {2}{G} · Instant
 *
 * The destroy resolves first, then the 1-damage sweep re-reads the flying creatures opponents
 * control from projected state. A targeted creature that survives the destroy (regenerated or
 * indestructible) and is still an opponent's flier takes the damage too, as printed.
 */
val SagittarsVolley = card("Sagittars' Volley") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Instant"
    oracleText = "Destroy target creature with flying. Sagittars' Volley deals 1 damage to each " +
        "creature with flying your opponents control."

    spell {
        val flier = target(TargetFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING)))
        effect = Effects.Destroy(flier) then Patterns.Group.dealDamageToAll(
            1,
            GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING).opponentControls())
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "138"
        artist = "Dan Murayama Scott"
        flavorText = "\"My bowstring hums in tune with the song of the Worldsoul.\"\n" +
            "—Alcarus, Selesnya sagittar"
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d3104cad-e684-4bd7-b26b-5aa862f7a2b3.jpg"
    }
}
