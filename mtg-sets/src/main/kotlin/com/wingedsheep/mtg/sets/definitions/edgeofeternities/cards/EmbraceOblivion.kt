package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanent

/**
 * Embrace Oblivion
 * {B}
 * Sorcery
 * As an additional cost to cast this spell, sacrifice an artifact or creature.
 * Destroy target creature or Spacecraft.
 */
val EmbraceOblivion = card("Embrace Oblivion") {
    manaCost = "{B}"
    typeLine = "Sorcery"
    oracleText = "As an additional cost to cast this spell, sacrifice an artifact or creature.\nDestroy target creature or Spacecraft."

    // Additional cost: sacrifice an artifact or creature
    additionalCost(AdditionalCost.SacrificePermanent(GameObjectFilter.Artifact.or(GameObjectFilter.Creature)))

    spell {
        val t = target("target", TargetPermanent(filter = TargetFilter(GameObjectFilter.Creature or GameObjectFilter.Permanent.withSubtype("Spacecraft"))))
        effect = MoveToZoneEffect(t, Zone.GRAVEYARD, byDestruction = true)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "98"
        artist = "Andreas Zafiratos"
        flavorText = "\"Join me in the Next Eternity.\""
        imageUri = "https://cards.scryfall.io/normal/front/3/7/3754fc20-7aaa-437d-97df-d6cf1c29586c.jpg?1752946952"
    }
}
