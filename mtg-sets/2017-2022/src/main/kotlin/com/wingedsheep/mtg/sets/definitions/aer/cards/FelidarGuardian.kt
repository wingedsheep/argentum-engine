package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Felidar Guardian
 * {3}{W}
 * Creature — Cat Beast
 * 1/4
 * When this creature enters, you may exile another target permanent you control, then return that
 * card to the battlefield under its owner's control.
 *
 * Restoration Angel's optional blink with Kelpie Guide's "another target permanent you control"
 * (`excludeSelf`). Moving the exiled card back to the battlefield returns it under its owner's
 * control as a new object (CR 400.7); a token exiled this way ceases to exist and doesn't return.
 */
val FelidarGuardian = card("Felidar Guardian") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Cat Beast"
    power = 1
    toughness = 4
    oracleText = "When this creature enters, you may exile another target permanent you control, " +
        "then return that card to the battlefield under its owner's control."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val permanent = target(TargetFilter.Permanent.youControl().copy(excludeSelf = true))
        effect = Effects.May(
            Effects.Move(permanent, Zone.EXILE) then Effects.Move(permanent, Zone.BATTLEFIELD)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "19"
        artist = "Jakub Kasper"
        flavorText = "The loyalty of a felidar is hard won but worth the effort."
        imageUri = "https://cards.scryfall.io/normal/front/4/4/44bdbed8-5d21-4bf5-8a32-9623b1139c85.jpg?1783936780"
        ruling(
            "2017-02-09",
            "If a creature token is exiled this way, it will cease to exist and will not return to the battlefield."
        )
        ruling(
            "2017-02-09",
            "After the permanent returns to the battlefield, it will be a new object with no connection to the " +
                "permanent that was exiled. It won't have any additional abilities it may have had when it was " +
                "exiled. Any +1/+1 counters on it or Auras attached to it are removed, and any Equipment will no " +
                "longer be attached."
        )
    }
}
