package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyDamageAmount
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Jaya, Venerated Firemage
 * {4}{R}
 * Legendary Planeswalker — Jaya
 * Loyalty: 5
 *
 * If another red source you control would deal damage to a permanent or player, it deals that
 * much damage plus 1 to that permanent or player instead.
 * −2: Jaya deals 2 damage to any target.
 *
 * Torbran's shape — a plain additive [ModifyDamageAmount], so the extra point is dealt by the
 * original source (ruling) — with the source scoped to "another red source you control" through
 * `notSourceItself()` (the damage-source filter is evaluated with Jaya as the host), and the
 * recipient widened to [Recipient.Any]: any permanent or player, yourself included. Jaya's own −2
 * is dealt by Jaya and so is not boosted.
 */
val JayaVeneratedFiremage = card("Jaya, Venerated Firemage") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Planeswalker — Jaya"
    startingLoyalty = 5
    oracleText = "If another red source you control would deal damage to a permanent or player, it deals " +
        "that much damage plus 1 to that permanent or player instead.\n" +
        "−2: Jaya deals 2 damage to any target."

    replacementEffect(
        ModifyDamageAmount(
            modifier = 1,
            appliesTo = EventPattern.DamageEvent(
                source = GameObjectFilter.Any.withColor(Color.RED).youControl().notSourceItself(),
                recipient = Recipient.Any,
            ),
        )
    )

    loyaltyAbility(-2) {
        val victim = target(Targets.Any)
        effect = Effects.DealDamage(2, victim)
        description = "Jaya deals 2 damage to any target."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "135"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/1/2/129dc8a8-9fbc-4300-b559-243395dc6ed5.jpg?1783933424"
        ruling("2019-05-03", "Jaya's first ability doesn't cause Jaya to deal damage; it affects the amount of damage dealt by the original red source.")
        ruling("2019-05-03", "If another effect modifies how much damage your red source would deal, including preventing some of it, the player being dealt damage or the controller of the permanent being dealt damage chooses an order in which to apply those effects. If all of the damage is prevented, Jaya's effect no longer applies.")
        ruling("2019-05-03", "If damage dealt by a source you control is being divided or assigned among multiple permanents an opponent controls or among an opponent and one or more permanents they control simultaneously, divide the original amount before adding 1. For example, if you attack with a 5/5 red creature with trample and your opponent blocks with a 2/2 creature, you can assign 2 damage to the blocker and 3 damage to the defending player. These amounts are then modified to 3 and 4, respectively.")
    }
}
