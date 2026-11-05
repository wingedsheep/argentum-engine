package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Wretched Camel — Hour of Devastation #82 (canonical printing)
 * {1}{B} · Creature — Zombie Camel · 2/1
 *
 * When this creature dies, if you control a Desert or there is a Desert card in your graveyard,
 * target player discards a card.
 *
 * The Desert clause is an intervening-if (CR 603.4): checked as the dies trigger fires and again
 * on resolution, and either half may satisfy each check.
 */
val WretchedCamel = card("Wretched Camel") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Camel"
    power = 2
    toughness = 1
    oracleText = "When this creature dies, if you control a Desert or there is a Desert card in your graveyard, " +
        "target player discards a card."

    triggeredAbility {
        trigger = Triggers.self.dies()
        interveningIf = Conditions.Any(
            Conditions.ControlPermanentOfType(Subtype.DESERT),
            Conditions.GraveyardContainsSubtype(Subtype.DESERT),
        )
        val player = target(Targets.Player)
        effect = Effects.Discard(1, player)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "82"
        artist = "Dan Murayama Scott"
        flavorText = "Perfectly adapted to cross the wastes in life, they bring the wastes with them in death."
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cdd729df-8a02-4d5e-8432-bc0d006b0d3d.jpg?1783936034"
        ruling("2017-07-14", "If an ability checks whether you control a Desert or there is a Desert card in your graveyard, having more than one doesn’t matter. Controlling one is the same as controlling five. There is also no extra bonus for both controlling one and having one in your graveyard.")
        ruling("2017-07-14", "For abilities that trigger only if you control a Desert or there is a Desert card in your graveyard, one condition must be true as the ability triggers and one must be true as the ability resolves. They don’t have to be the same condition, though. For example, you could sacrifice your only Desert after the ability triggers but before it has resolved.")
    }
}
