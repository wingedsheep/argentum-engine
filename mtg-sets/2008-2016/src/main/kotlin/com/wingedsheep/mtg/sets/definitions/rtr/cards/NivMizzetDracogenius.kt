package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Niv-Mizzet, Dracogenius (RTR #183) — {2}{U}{U}{R}{R} Legendary Creature — Dragon Wizard 5/5.
 *
 *   Flying
 *   Whenever Niv-Mizzet deals damage to a player, you may draw a card.
 *   {U}{R}: Niv-Mizzet deals 1 damage to any target.
 *
 * Any damage to any player counts (combat or the ping, including to yourself).
 */
val NivMizzetDracogenius = card("Niv-Mizzet, Dracogenius") {
    manaCost = "{2}{U}{U}{R}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Dragon Wizard"
    oracleText = "Flying\n" +
        "Whenever Niv-Mizzet deals damage to a player, you may draw a card.\n" +
        "{U}{R}: Niv-Mizzet deals 1 damage to any target."
    power = 5
    toughness = 5

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsDamage(Recipient.AnyPlayer)
        optional = true
        effect = Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Mana("{U}{R}")
        val victim = target(Targets.Any)
        effect = Effects.DealDamage(1, victim)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "183"
        artist = "Todd Lockwood"
        flavorText = "He has no patience for minds that do not inspire him or explode by trying."
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c345e475-8095-41b5-90b4-771fcf80b939.jpg?1783940334"
    }
}
