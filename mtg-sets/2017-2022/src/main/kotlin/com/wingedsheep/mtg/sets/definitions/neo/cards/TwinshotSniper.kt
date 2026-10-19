package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Twinshot Sniper — Kamigawa: Neon Dynasty #168 (canonical printing)
 * {3}{R} · Artifact Creature — Goblin Archer · 2/3
 *
 * Reach
 * When this creature enters, it deals 2 damage to any target.
 * Channel — {1}{R}, Discard this card: It deals 2 damage to any target.
 */
val TwinshotSniper = card("Twinshot Sniper") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Artifact Creature — Goblin Archer"
    power = 2
    toughness = 3
    oracleText = "Reach\n" +
        "When this creature enters, it deals 2 damage to any target.\n" +
        "Channel — {1}{R}, Discard this card: It deals 2 damage to any target."

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val t = target(Targets.Any)
        effect = Effects.DealDamage(2, t)
        description = "When this creature enters, it deals 2 damage to any target."
    }

    // Channel — {1}{R}, Discard this card (from hand): the discarded card is the damage source.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{R}"), Costs.DiscardSelf)
        activateFromZone = Zone.HAND
        val t = target(Targets.Any)
        effect = Effects.DealDamage(2, t)
        description = "Channel — {1}{R}, Discard this card: It deals 2 damage to any target."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "168"
        artist = "Brent Hollowell"
        flavorText = "\"Pierce the heart and pierce the head, even the mightiest fall down dead.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/8/08a86009-4637-4b6c-9d36-367151583668.jpg?1783923857"
        ruling("2022-02-18", "Discarding the card is part of the cost to activate a channel ability.")
        ruling("2022-02-18", "If a channel ability requires a target, you may not activate it without a target just to discard the card.")
    }
}
