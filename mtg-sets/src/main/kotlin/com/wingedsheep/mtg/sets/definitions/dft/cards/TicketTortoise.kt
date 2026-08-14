package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Ticket Tortoise
 * {2}
 * Artifact Creature — Turtle — Common (DFT #245)
 * 3/1
 *
 * Defender
 * When this creature enters, if an opponent controls more lands than you, you create a Treasure
 * token.
 *
 * The enters ability carries an intervening-if clause (CR 603.4), modelled as
 * `triggerCondition = `[Conditions.OpponentControlsMoreLands] — the land comparison is checked both
 * as the trigger would go on the stack and again as it resolves, so a land drop in between (or an
 * opponent losing lands) can still make it do nothing.
 */
val TicketTortoise = card("Ticket Tortoise") {
    manaCost = "{2}"
    typeLine = "Artifact Creature — Turtle"
    power = 3
    toughness = 1
    oracleText = "Defender\n" +
        "When this creature enters, if an opponent controls more lands than you, you create a " +
        "Treasure token. (It's an artifact with \"{T}, Sacrifice this token: Add one mana of any " +
        "color.\")"

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.OpponentControlsMoreLands
        effect = Effects.CreateTreasure(1)
        description = "When this creature enters, if an opponent controls more lands than you, you " +
            "create a Treasure token."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "245"
        artist = "Brian Valeza"
        flavorText = "\"ADMIT ONE. ENJOY THE RACE.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/a/fa178ed7-8f3a-45f0-817d-5fbc7993b04a.jpg?1783907846"
    }
}
