package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Tomb Trawler
 * {2}
 * Artifact Creature — Golem
 * 0/4
 *
 * {2}: Put target card from your graveyard on the bottom of your library.
 */
val TombTrawler = card("Tomb Trawler") {
    manaCost = "{2}"
    colorIdentity = ""
    typeLine = "Artifact Creature — Golem"
    power = 0
    toughness = 4
    oracleText = "{2}: Put target card from your graveyard on the bottom of your library."

    activatedAbility {
        cost = Costs.Mana("{2}")
        val t = target(
            "target",
            TargetObject(
                filter = TargetFilter(
                    baseFilter = GameObjectFilter.Any.ownedByYou(),
                    zone = Zone.GRAVEYARD,
                ),
            ),
        )
        effect = Effects.Move(
            target = t,
            destination = Zone.LIBRARY,
            placement = ZonePlacement.Bottom,
        )
        description = "{2}: Put target card from your graveyard on the bottom of your library."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "250"
        artist = "Anton Solovianchyk"
        flavorText = "\"Dying is common on Thunder Junction. Resting in peace is a much rarer commodity.\"\n" +
            "—Baron Bertram Graywater"
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36e61cb8-219a-4fe6-a2e6-307665ffa38f.jpg?1712356295"
    }
}
