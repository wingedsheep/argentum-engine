package com.wingedsheep.mtg.sets.definitions.mor.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender

/**
 * Preeminent Captain
 * {2}{W}
 * Creature — Kithkin Soldier
 * 2/2
 * First strike
 * Whenever this creature attacks, you may put a Soldier creature card from your hand onto the
 * battlefield tapped and attacking.
 *
 * A bare "tapped and attacking": the controller chooses what the Soldier attacks (CR 508.4; the
 * 2014-07-18 ruling says it needn't match the Captain's defender), hence
 * [EnteringAttackDefender.CONTROLLER_CHOOSES] — the same shape as Minion of the Mighty.
 */
val PreeminentCaptain = card("Preeminent Captain") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kithkin Soldier"
    power = 2
    toughness = 2
    oracleText = "First strike (This creature deals combat damage before creatures without first strike.)\n" +
        "Whenever this creature attacks, you may put a Soldier creature card from your hand onto the " +
        "battlefield tapped and attacking."

    keywords(Keyword.FIRST_STRIKE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Patterns.Hand.putFromHand(
            filter = GameObjectFilter.Creature.withSubtype("Soldier"),
            entersAttacking = true,
            attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "20"
        artist = "Greg Staples"
        flavorText = "\"If you need an example to lead others to the front lines, consider the precedent set.\""
        imageUri = "https://cards.scryfall.io/normal/front/4/1/41659773-8b26-41bf-afcc-805f373c0074.jpg?1783942802"

        ruling("2014-07-18", "You choose what the Soldier you put onto the battlefield is attacking. It doesn't have to attack the same player or planeswalker as Preeminent Captain.")
        ruling("2014-07-18", "Any abilities of the Soldier creature card that trigger \"Whenever [this creature] attacks\" won't trigger because the creature was never declared as an attacking creature.")
    }
}
