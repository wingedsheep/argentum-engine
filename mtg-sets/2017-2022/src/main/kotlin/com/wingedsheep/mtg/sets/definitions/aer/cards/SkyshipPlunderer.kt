package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Skyship Plunderer — Aether Revolt #46 (canonical printing)
 * {1}{U} · Creature — Human Pirate · 2/1
 *
 * Flying
 * Whenever this creature deals combat damage to a player, for each kind of counter on target
 * permanent or player, give that permanent or player another counter of that kind.
 *
 * The rider is the targeted, single-object form of proliferate's placement rule —
 * `Effects.Proliferate(target = …)` (Powerful Broker's shape). The recipient is a real target
 * (`Targets.PermanentOrPlayer`), chosen as the trigger goes on the stack and independent of which
 * player was dealt damage; a player recipient gets one more of each kind they have (energy,
 * poison, experience). It isn't a proliferate, so "whenever you proliferate" doesn't fire.
 */
val SkyshipPlunderer = card("Skyship Plunderer") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Pirate"
    power = 2
    toughness = 1
    oracleText = "Flying\nWhenever this creature deals combat damage to a player, for each kind of counter " +
        "on target permanent or player, give that permanent or player another counter of that kind."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        val recipient = target(Targets.PermanentOrPlayer)
        effect = Effects.Proliferate(recipient)
        description = "Whenever this creature deals combat damage to a player, for each kind of counter on " +
            "target permanent or player, give that permanent or player another counter of that kind."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "46"
        artist = "Slawomir Maniak"
        flavorText = "Escapes should always be daring."
        imageUri = "https://cards.scryfall.io/normal/front/1/f/1fedbc91-67e4-40d6-b307-7e6197f47c6e.jpg?1783936769"
        ruling(
            "2017-02-09",
            "Skyship Plunderer's triggered ability can target any permanent or player, regardless of which " +
                "player was dealt combat damage."
        )
        ruling(
            "2017-02-09",
            "Skyship Plunderer's triggered ability gives only one counter of each kind. It doesn't double the " +
                "number of each kind of counter. For example, if a creature has two +1/+1 counters and a charge " +
                "counter on it, it gets one +1/+1 counter and one charge counter."
        )
        ruling(
            "2017-02-09",
            "To give a counter is to put a counter on a permanent or to have a player get a counter. Effects " +
                "that interact with a player getting counters or counters being placed on permanents interact " +
                "with Skyship Plunderer's triggered ability."
        )
    }
}
