package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Restoration Gearsmith
 * {2}{W}{B}
 * Creature — Human Artificer
 * 3/3
 * When this creature enters, return target artifact or creature card from your graveyard to your hand.
 */
val RestorationGearsmith = card("Restoration Gearsmith") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "BW"
    typeLine = "Creature — Human Artificer"
    oracleText = "When this creature enters, return target artifact or creature card from your graveyard to your hand."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        val card = target(TargetFilter((GameObjectFilter.Artifact or GameObjectFilter.Creature).ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.Move(target = card, destination = Zone.HAND)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "185"
        artist = "John Severin Brassell"
        flavorText = "All she touches turns to well-oiled, delicately calibrated, perfectly balanced gold."
        imageUri = "https://cards.scryfall.io/normal/front/2/2/228ea9c6-5732-4a2a-ac25-a768ea7d433b.jpg?1783937166"
    }
}
