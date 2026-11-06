package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fairgrounds Warden
 * {2}{W}
 * Creature — Dwarf Soldier
 * 1/3
 * When this creature enters, exile target creature an opponent controls until this creature leaves the battlefield.
 */
val FairgroundsWarden = card("Fairgrounds Warden") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dwarf Soldier"
    oracleText = "When this creature enters, exile target creature an opponent controls until this creature leaves the battlefield."
    power = 1
    toughness = 3

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter(GameObjectFilter.Creature.opponentControls()))
        effect = Effects.ExileUntilLeaves(creature)
    }

    triggeredAbility {
        trigger = Triggers.self.leaves()
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "13"
        artist = "Izzy"
        flavorText = "\"You're coming with me.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/9/49b6c8f3-76b7-4954-8608-023304cdec3e.jpg?1783937234"
    }
}
