package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Displace
 * {2}{U}
 * Instant
 * Exile up to two target creatures you control, then return those cards to the battlefield under
 * their owner's control.
 *
 * The same linked-exile blink as Illusionist's Stratagem: the chosen targets are exiled together
 * (linked to this spell), then the whole batch returns at once under each card's owner's control as
 * new objects. An illegal target is simply not exiled while the other is still flickered; a token
 * ceases to exist in exile and never returns.
 */
val Displace = card("Displace") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Exile up to two target creatures you control, then return those cards to the " +
        "battlefield under their owner's control."

    spell {
        targets(TargetFilter.Creature.youControl(), count = 2, optional = true)
        effect = Effects.Pipeline(
            descriptionOverride = "Exile up to two target creatures you control, then return those " +
                "cards to the battlefield under their owner's control"
        ) {
            val chosen = gather(source = CardSource.ChosenTargets)
            exile(chosen, linkToSource = true)
            val returning = gather(source = CardSource.FromLinkedExile())
            move(returning, CardDestination.ToZone(Zone.BATTLEFIELD), underOwnersControl = true)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "55"
        artist = "Clint Cearley"
        flavorText = "\"I'll see you soon, Subjects 25 and 26. I've got a good feeling this time!\""
        imageUri = "https://cards.scryfall.io/normal/front/8/a/8ab850c5-6f5e-41b7-ab52-094579caca12.jpg?1783937502"
        ruling(
            "2016-07-13",
            "If a creature token is exiled this way, it will cease to exist and will not return to the battlefield."
        )
        ruling(
            "2016-07-13",
            "After each creature returns to the battlefield, it will be a new object with no connection to the " +
                "creature that was exiled. It won't be in combat or have any additional abilities it may have had " +
                "when it was exiled. Any counters on it cease to exist; Auras attached to it are put into their " +
                "owner's graveyards; and any Equipment will become unattached."
        )
    }
}
