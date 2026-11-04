package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.DoubleDamage
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.DamageType
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Charging Tuskodon — Rivals of Ixalan #97 (canonical printing)
 * {3}{R}{R} · Creature — Dinosaur 4/4
 *
 * A [DoubleDamage] replacement (CR 616) scoped to this creature's own combat damage
 * (`GameObjectFilter.Any.sourceItself()`, `DamageType.Combat`) to a player. Damage to blockers and
 * planeswalkers is dealt normally. Trample assignment uses its actual power and is doubled only as
 * it's dealt; the doubled damage is still combat damage dealt by Charging Tuskodon.
 */
val ChargingTuskodon = card("Charging Tuskodon") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dinosaur"
    power = 4
    toughness = 4
    oracleText = "Trample\n" +
        "If this creature would deal combat damage to a player, it deals double that damage to that player instead."

    keywords(Keyword.TRAMPLE)

    replacementEffect(
        DoubleDamage(
            appliesTo = EventPattern.DamageEvent(
                recipient = Recipient.AnyPlayer,
                source = GameObjectFilter.Any.sourceItself(),
                damageType = DamageType.Combat,
            )
        )
    )

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "Izzy"
        flavorText = "It doesn't hold grudges. It runs them down and impales them."
        imageUri = "https://cards.scryfall.io/normal/front/8/e/8e49960d-4b45-4d3b-9c6e-e7b717b4feaa.jpg?1783935301"
        ruling("2018-01-19", "If Charging Tuskodon has trample damage to assign to a player, that damage is assigned based on its actual power and is doubled only as it's dealt. For example, if Charging Tuskodon is blocked by a 3/3 creature, the attacking player can assign 1 damage to the defending player and then Charging Tuskodon deals 2 damage to that player.")
        ruling("2018-01-19", "The doubled damage Charging Tuskodon deals is still combat damage.")
    }
}
