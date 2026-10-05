package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Wispweaver Angel
 * {4}{W}{W}
 * Creature — Angel
 * 4/4
 * Flying
 * When this creature enters, you may exile another target creature you control, then return that
 * card to the battlefield under its owner's control.
 *
 * The target is chosen when the trigger goes on the stack; the "may" is answered on resolution.
 * The return is a plain move to the battlefield, which puts the card under its owner's control; an
 * exiled token ceases to exist and doesn't come back.
 */
val WispweaverAngel = card("Wispweaver Angel") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "When this creature enters, you may exile another target creature you control, then return " +
        "that card to the battlefield under its owner's control."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val creature = target(TargetFilter.OtherCreatureYouControl)
        effect = Effects.May(
            Effects.Move(creature, Zone.EXILE) then Effects.Move(creature, Zone.BATTLEFIELD)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "35"
        artist = "James Ryman"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57300d02-faad-43b2-afa9-023d1c3a0901.jpg?1783937225"
        ruling("2016-09-20", "Auras attached to the exiled creature will be put into their owners' graveyards. Equipment attached to the exiled creature will become unattached and remain on the battlefield. Any counters on the exiled creature will cease to exist.")
        ruling("2016-09-20", "If a creature token is exiled this way, it will cease to exist and won't return to the battlefield.")
        ruling("2016-09-20", "Wispweaver Angel's triggered ability can target another Wispweaver Angel. If so, the two Angels can loop in and out of exile as many times as you'd like before you choose to target another creature or not to use the triggered ability.")
    }
}
