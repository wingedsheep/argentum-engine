package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CompanionCondition
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Gyruda, Doom of Depths — Ikoria: Lair of Behemoths #221
 * {4}{U/B}{U/B} · Legendary Creature — Demon Kraken · 6/6
 *
 * Companion — every card in the starting deck has an even mana value
 * ([CompanionCondition.EachCard] over `Any.manaValueIsEven()`; mana value 0 is even).
 *
 * The ETB mills every player through the pipeline `mill`, which keeps the milled cards as one
 * collection across all libraries — so the pick is "from among the milled cards" only, never a card
 * already in a graveyard, and it may be an opponent's card. Holding the cards by identity also
 * covers the ruling that a card exiled instead by a replacement effect is still choosable. The put
 * is mandatory (`chooseExactly(1)`) and does nothing when no even-mana-value creature was milled.
 */
val GyrudaDoomOfDepths = card("Gyruda, Doom of Depths") {
    manaCost = "{4}{U/B}{U/B}"
    typeLine = "Legendary Creature — Demon Kraken"
    power = 6
    toughness = 6
    oracleText = "Companion — Your starting deck contains only cards with even mana values. " +
        "(If this card is your chosen companion, you may put it into your hand from outside the game " +
        "for {3} as a sorcery.)\n" +
        "When Gyruda enters, each player mills four cards. Put a creature card with an even mana value " +
        "from among the milled cards onto the battlefield under your control."

    keywordAbility(KeywordAbility.companion(CompanionCondition.EachCard(GameObjectFilter.Any.manaValueIsEven())))

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            val milled = mill(4, Player.Each)
            val chosen = chooseExactly(
                1,
                from = milled,
                filter = GameObjectFilter.Creature.manaValueIsEven(),
                prompt = "Put a creature card with an even mana value from among the milled cards onto the battlefield under your control",
                selectedLabel = "Put onto the battlefield",
                remainderLabel = "Leave",
            )
            move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
        }
        description = "When Gyruda enters, each player mills four cards. Put a creature card with an even " +
            "mana value from among the milled cards onto the battlefield under your control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "221"
        artist = "Tyler Jacobson"
        imageUri = "https://cards.scryfall.io/normal/front/9/7/97eb1804-6fd8-4917-af36-87fdfce39d3a.jpg?1783931012"
        ruling("2020-04-17", "If a card in a player's graveyard has {X} in its mana cost, X is considered to be 0.")
        ruling("2020-04-17", "A card with mana value 0 has an even mana value.")
        ruling("2020-04-17", "If a replacement effect causes a player to exile the top four cards of their library instead of putting them into their graveyard as Gyruda's triggered ability resolves, the creature card you choose may be one of those cards in exile.")
    }
}
