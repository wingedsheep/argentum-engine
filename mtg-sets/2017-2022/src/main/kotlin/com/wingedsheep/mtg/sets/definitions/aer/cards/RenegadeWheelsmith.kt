package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Renegade Wheelsmith
 * {1}{R}{W}
 * Creature — Dwarf Pilot
 * 3/2
 * Whenever this creature becomes tapped, target creature can't block this turn.
 */
val RenegadeWheelsmith = card("Renegade Wheelsmith") {
    manaCost = "{1}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Creature — Dwarf Pilot"
    oracleText = "Whenever this creature becomes tapped, target creature can't block this turn."
    power = 3
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.becomesTapped()
        val creature = target(TargetFilter.Creature)
        effect = Effects.CantBlock(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "134"
        artist = "Darek Zabrocki"
        flavorText = "Many veterans of the Ovalchase racetrack turned their talents to the renegade cause."
        imageUri = "https://cards.scryfall.io/normal/front/6/9/6956a341-10fd-48af-b382-88c2b2934a3e.jpg"
        ruling("2017-02-09", "This is a triggered ability, not an activated ability. It doesn't allow you to tap Renegade Wheelsmith whenever you want; rather, you need some other way of tapping it, such as by attacking or crewing a Vehicle.")
        ruling("2017-02-09", "For the ability to trigger, Renegade Wheelsmith has to actually change from untapped to tapped. If an effect attempts to tap it, but it was already tapped at the time, this ability won't trigger.")
    }
}
