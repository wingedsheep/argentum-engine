package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompanionCondition
import com.wingedsheep.sdk.scripting.DoubleDamage
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.events.Recipient

/**
 * Obosh, the Preypiercer — Ikoria: Lair of Behemoths #228
 * {3}{B/R}{B/R} · Legendary Creature — Hellion Horror · 3/5
 *
 * Companion — every card in the starting deck is a land or has an odd mana value
 * ([CompanionCondition.EachCard] over `Land or Any.manaValueIsOdd()`).
 *
 * The doubler is [DoubleDamage] scoped to a source you control whose mana value is odd, read
 * when the damage would be dealt: a spell on the stack counts its announced X (Blaze with X=2 is
 * mana value 3), while lands and non-copy tokens have mana value 0 and are never doubled.
 */
val OboshThePreypiercer = card("Obosh, the Preypiercer") {
    manaCost = "{3}{B/R}{B/R}"
    typeLine = "Legendary Creature — Hellion Horror"
    power = 3
    toughness = 5
    oracleText = "Companion — Your starting deck contains only cards with odd mana values and land cards. " +
        "(If this card is your chosen companion, you may put it into your hand from outside the game " +
        "for {3} as a sorcery.)\n" +
        "If a source you control with an odd mana value would deal damage to a permanent or player, " +
        "it deals double that damage to that permanent or player instead."

    keywordAbility(
        KeywordAbility.companion(
            CompanionCondition.EachCard(GameObjectFilter.Land or GameObjectFilter.Any.manaValueIsOdd())
        )
    )

    replacementEffect(
        DoubleDamage(
            appliesTo = EventPattern.DamageEvent(
                source = GameObjectFilter.Any.youControl().manaValueIsOdd(),
                recipient = Recipient.Any,
            ),
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "228"
        artist = "Daarken"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/451507de-9c42-43ee-b9ba-1f69e9aa29d2.jpg?1783931009"
        ruling("2023-04-14", "The damage is dealt by the same source as the original source of damage. The doubled damage isn't dealt by Obosh unless it was the original source of damage.")
        ruling("2020-04-17", "For spells with {X} in their mana costs, use the value chosen for X to determine the spell's mana value. If a permanent or card in any other zone has {X} in its mana cost, X is considered to be 0.")
        ruling("2020-04-17", "If a creature with trample you control would deal combat damage to a blocking creature while you control Obosh, you must assign its unmodified damage. For example, a 3/3 creature with trample blocked by a 2/2 creature can have 1 damage assigned to the defending player. It will then deal 4 damage to the blocking creature and 2 damage to the defending player.")
        ruling("2020-04-17", "If an effect such as that of Ravenous Gigantotherium asks you to divide damage among targets, you must divide the unmodified damage before doubling it.")
        ruling("2020-04-17", "If multiple replacement or prevention effects try to modify damage that would be dealt to a permanent or player, the player or the controller of the permanent chooses the order in which they apply.")
    }
}
