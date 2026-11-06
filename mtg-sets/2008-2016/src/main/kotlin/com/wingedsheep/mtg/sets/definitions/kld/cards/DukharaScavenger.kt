package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Dukhara Scavenger
 * {5}{B}
 * Creature — Crocodile
 * 4/6
 * When this creature enters, you may put target artifact or creature card from your graveyard on top of your library.
 */
val DukharaScavenger = card("Dukhara Scavenger") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Crocodile"
    oracleText = "When this creature enters, you may put target artifact or creature card from your graveyard on top of your library."
    power = 4
    toughness = 6

    triggeredAbility {
        trigger = Triggers.self.enters()
        val card = target(TargetFilter((GameObjectFilter.Artifact or GameObjectFilter.Creature).ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.May(Effects.PutOnTopOfLibrary(card))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "77"
        artist = "Sidharth Chaturvedi"
        flavorText = "The teeth marks add character."
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81591264-1342-418d-b5c6-9d700b729c51.jpg?1783937210"
    }
}
