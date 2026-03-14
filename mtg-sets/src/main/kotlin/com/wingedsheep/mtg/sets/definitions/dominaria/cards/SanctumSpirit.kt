package com.wingedsheep.mtg.sets.definitions.dominaria.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sanctum Spirit
 * {3}{W}
 * Creature — Spirit
 * 3/2
 * Lifelink
 * Discard a historic card: This creature gains indestructible until end of turn.
 * (Artifacts, legendaries, and Sagas are historic.)
 */
val SanctumSpirit = card("Sanctum Spirit") {
    manaCost = "{3}{W}"
    typeLine = "Creature — Spirit"
    power = 3
    toughness = 2
    oracleText = "Lifelink\nDiscard a historic card: Sanctum Spirit gains indestructible until end of turn. (Artifacts, legendaries, and Sagas are historic.)"

    keywords(Keyword.LIFELINK)

    activatedAbility {
        cost = Costs.Discard(GameObjectFilter.Historic)
        effect = Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "30"
        artist = "Nils Hamm"
        flavorText = "When history becomes too mournful, even good souls may choose to forget."
        imageUri = "https://cards.scryfall.io/normal/front/a/a/aa4795dd-75d4-4ce5-87e7-fe892616e42e.jpg?1591104413"
    }
}
