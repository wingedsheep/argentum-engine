package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompanionCondition
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Keruga, the Macrosage — Ikoria: Lair of Behemoths #225
 * {3}{G/U}{G/U} · Legendary Creature — Dinosaur Hippo · 5/4
 *
 * Companion — Your starting deck contains only cards with mana value 3 or greater and land cards
 * (CR 702.139; X counts as 0 — ruling).
 * The enters trigger counts the *other* permanents you control as it resolves.
 */
val KerugaTheMacrosage = card("Keruga, the Macrosage") {
    manaCost = "{3}{G/U}{G/U}"
    typeLine = "Legendary Creature — Dinosaur Hippo"
    power = 5
    toughness = 4
    oracleText = "Companion — Your starting deck contains only cards with mana value 3 or greater and land " +
        "cards. (If this card is your chosen companion, you may put it into your hand from outside the game " +
        "for {3} as a sorcery.)\n" +
        "When Keruga enters, draw a card for each other permanent you control with mana value 3 or greater."

    keywordAbility(
        KeywordAbility.companion(
            CompanionCondition.EachCard(GameObjectFilter.Land or GameObjectFilter.Any.manaValueAtLeast(3))
        )
    )

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(
            DynamicAmounts.battlefield(
                Player.You, GameObjectFilter.Permanent.manaValueAtLeast(3), excludeSelf = true,
            ).count()
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "225"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/a/9/a90ee952-de7a-420f-993c-a38db89bc8ac.jpg?1783931011"
        ruling("2020-04-17", "If a card in a player's deck or a permanent on the battlefield has {X} in its mana cost, X is considered to be 0.")
        ruling("2020-04-17", "The mana value of a token that isn't a copy of another object is 0. A token that is a copy of another object has the same mana cost as that object.")
    }
}
