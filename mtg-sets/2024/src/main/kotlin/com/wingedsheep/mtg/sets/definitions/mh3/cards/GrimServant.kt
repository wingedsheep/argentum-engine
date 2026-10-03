package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Grim Servant {3}{B}
 * Creature — Zombie Warlock
 * 3/2
 * Menace
 * When this creature enters, search your library for a card with mana value less than or equal to
 * your devotion to black, reveal it, put it into your hand, then shuffle. You lose 3 life.
 *
 * Devotion is read as the trigger resolves (per the ruling), so the cap is the dynamic mana-value
 * predicate over [DynamicAmounts.devotionTo]. The life loss is unconditional — it happens even if
 * the search finds nothing.
 */
val GrimServant = card("Grim Servant") {
    manaCost = "{3}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie Warlock"
    oracleText = "Menace\n" +
        "When this creature enters, search your library for a card with mana value less than or equal to " +
        "your devotion to black, reveal it, put it into your hand, then shuffle. You lose 3 life. " +
        "(Each {B} in the mana costs of permanents you control counts toward your devotion to black.)"
    power = 3
    toughness = 2

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Any.manaValueAtMostDynamic(DynamicAmounts.devotionTo(Color.BLACK)),
            destination = SearchDestination.HAND,
            reveal = true
        ) then Effects.LoseLife(3, EffectTarget.Controller)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "97"
        artist = "David Astruga"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/df9c275d-e8cc-40c2-8baf-7a0d154aa7cf.jpg?1783911279"
        ruling("2024-06-07", "Count the number of black mana symbols among the mana costs of permanents you control as Grim Servant's triggered ability resolves to determine your devotion to black. If Grim Servant is still on the battlefield at that time, it will be included in that count.")
        ruling("2024-06-07", "If a card in a player's library has {X} in its mana cost, X is 0 when determining that card's mana value.")
    }
}
