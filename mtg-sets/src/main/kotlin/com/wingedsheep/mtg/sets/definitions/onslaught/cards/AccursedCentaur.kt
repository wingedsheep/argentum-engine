package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect

/**
 * Accursed Centaur
 * {B}
 * Creature — Zombie Centaur
 * 2/2
 * When Accursed Centaur enters the battlefield, sacrifice a creature.
 */
val AccursedCentaur = card("Accursed Centaur") {
    manaCost = "{B}"
    typeLine = "Creature — Zombie Centaur"
    power = 2
    toughness = 2
    oracleText = "When Accursed Centaur enters the battlefield, sacrifice a creature."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = SacrificeEffect(GameObjectFilter.Creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "123"
        artist = "Jerry Tiritilli"
        flavorText = "\"The Cabal mocks the natural order. For its minions, death is just a pause between duties.\" —Kamahl, druid acolyte"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/894556d8-6d5c-431b-a45d-26cd37c5f456.jpg?1562927409"
    }
}
