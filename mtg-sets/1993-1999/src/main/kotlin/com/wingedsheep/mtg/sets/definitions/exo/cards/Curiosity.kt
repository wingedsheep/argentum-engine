package com.wingedsheep.mtg.sets.definitions.exo.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Curiosity
 * {U}
 * Enchantment — Aura
 * Enchant creature
 * Whenever enchanted creature deals damage to an opponent, you may draw a card.
 */
val Curiosity = card("Curiosity") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Whenever enchanted creature deals damage to an opponent, you may draw a card."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.attached.dealsDamage(Recipient.Opponent)
        effect = Effects.May(Effects.DrawCards(1))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "29"
        artist = "Val Mayerik"
        flavorText = "All Mirri wanted to do was rest, but she couldn't ignore a nagging suspicion as she followed Crovax's skulking form."
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fee17ef5-7e1a-42ae-b680-df81204df7dd.jpg?1783946527"
    }
}
