package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Hangar Scrounger
 * {2}{R}
 * Creature — Dwarf Pilot
 * 2/1
 * Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If that's another
 * creature, it gains the following ability until end of turn.)
 * Whenever this creature becomes tapped, you may discard a card. If you do, draw a card.
 */
val HangarScrounger = card("Hangar Scrounger") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dwarf Pilot"
    oracleText = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. If " +
        "that's another creature, it gains the following ability until end of turn.)\n" +
        "Whenever this creature becomes tapped, you may discard a card. If you do, draw a card."
    power = 2
    toughness = 1

    triggeredAbility {
        isBackup = true
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature) then
            Effects.If(
                condition = Conditions.Not(Conditions.TargetIsSource(0)),
                then = Effects.GrantTriggeredAbility(
                    ability = grantedTriggeredAbility {
                        trigger = Triggers.self.becomesTapped()
                        effect = Effects.May(
                            Patterns.Hand.discardCards(1) then Effects.DrawCards(1)
                        )
                    },
                    target = creature,
                ),
            )
        description = "Backup 1 (When this creature enters, put a +1/+1 counter on target creature. " +
            "If that's another creature, it gains the following ability until end of turn.)"
    }

    triggeredAbility {
        trigger = Triggers.self.becomesTapped()
        effect = Effects.May(
            Patterns.Hand.discardCards(1) then Effects.DrawCards(1)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "142"
        artist = "Borja Pindado"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2551b01-4d88-47f3-b54d-96ec03e093f8.jpg?1783916991"
    }
}
