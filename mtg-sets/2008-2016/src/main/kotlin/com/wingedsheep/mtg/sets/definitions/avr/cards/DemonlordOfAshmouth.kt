package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Demonlord of Ashmouth — Avacyn Restored #96.
 * {2}{B}{B} Creature — Demon 5/4
 *
 * The enters trigger is the Apocalypse Demon shape: [Effects.PayOrSuffer] with
 * [Costs.pay.SacrificeAnother] (the Demonlord itself is never a legal sacrifice) and the exile as the
 * "suffer" branch. Paying is optional even with another creature available. If the Demonlord has
 * already left the battlefield when the trigger resolves, "it" names a new object (CR 400.7) and the
 * exile does nothing. Undying returning it re-triggers the enters ability.
 */
val DemonlordOfAshmouth = card("Demonlord of Ashmouth") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    oracleText = "Flying\n" +
        "When this creature enters, exile it unless you sacrifice another creature.\n" +
        "Undying (When this creature dies, if it had no +1/+1 counters on it, return it to the " +
        "battlefield under its owner's control with a +1/+1 counter on it.)"
    power = 5
    toughness = 4
    keywords(Keyword.FLYING, Keyword.UNDYING)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.PayOrSuffer(
            cost = Costs.pay.SacrificeAnother(GameObjectFilter.Creature),
            suffer = Effects.Exile(EffectTarget.Self),
        )
        description = "When this creature enters, exile it unless you sacrifice another creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "96"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/7/8/785da9a3-09af-45aa-bc04-4ab69cfb2ba4.jpg?1783940701"
        ruling(
            "2012-05-01",
            "Demonlord of Ashmouth's \"enters\" ability will trigger no matter how it entered, including " +
                "because of its undying ability."
        )
        ruling(
            "2012-05-01",
            "You don't have to sacrifice another creature, even if you control one. You may choose to " +
                "exile Demonlord of Ashmouth."
        )
    }
}
