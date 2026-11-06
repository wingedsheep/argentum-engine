package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Deadeye Harpooner
 * {2}{W}
 * Creature — Dwarf Warrior
 * 2/2
 *
 * Revolt — When this creature enters, if a permanent left the battlefield under your control
 * this turn, destroy target tapped creature an opponent controls.
 *
 * Revolt is a CR 603.4 intervening-if (checked on entering and again on resolution), so it rides
 * on `interveningIf` with the per-player [Conditions.YouHadPermanentLeaveBattlefieldThisTurn]
 * tracker, as on Airdrop Aeronauts.
 *
 * Aether Revolt is the card's only printing.
 */
val DeadeyeHarpooner = card("Deadeye Harpooner") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Dwarf Warrior"
    oracleText = "Revolt — When this creature enters, if a permanent left the battlefield under your control this turn, destroy target tapped creature an opponent controls."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouHadPermanentLeaveBattlefieldThisTurn
        val creature = target(TargetFilter.TappedCreature.opponentControls())
        effect = Effects.Destroy(creature)
        description = "Revolt — When this creature enters, if a permanent left the battlefield under your control this turn, destroy target tapped creature an opponent controls."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "15"
        artist = "Ryan Pancoast"
        flavorText = "\"It seems there's even better hunting down here.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a018decf-25e1-45fc-be7d-2523dcfb7a4c.jpg?1783936780"
        ruling("2017-02-09", "All cards in the Aether Revolt set with triggered revolt abilities use an intervening \"if\" clause. A permanent you controlled must have left the battlefield earlier in the turn in order for these abilities to trigger; otherwise they do nothing.")
        ruling("2017-02-09", "Revolt abilities don't care why the permanent left the battlefield, who caused it to move, or where it moved to.")
        ruling("2017-02-09", "Tokens that leave the battlefield will satisfy a revolt ability.")
        ruling("2017-02-09", "Energy counters aren't permanents. Paying {E} won't satisfy a revolt ability.")
    }
}
