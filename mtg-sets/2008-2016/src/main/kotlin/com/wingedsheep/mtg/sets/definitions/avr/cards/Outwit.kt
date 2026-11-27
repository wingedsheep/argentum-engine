package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Outwit
 * {U}
 * Instant
 * Counter target spell that targets a player.
 *
 * "That targets a player" is [CardPredicate.TargetsPlayer] with [Player.Any] on the stack target:
 * any player, either side; the spell may have other targets too (ruling).
 */
val Outwit = card("Outwit") {
    manaCost = "{U}"
    typeLine = "Instant"
    oracleText = "Counter target spell that targets a player."

    spell {
        target(
            TargetFilter(
                GameObjectFilter(cardPredicates = listOf(CardPredicate.TargetsPlayer(Player.Any))),
                zone = Zone.STACK,
            )
        )
        effect = Effects.CounterSpell()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "70"
        artist = "Erica Yang"
        flavorText = "The student asks how a spell is begun. The master ponders what ends it."
        imageUri = "https://cards.scryfall.io/normal/front/4/2/429f7cf0-579a-4003-b5cf-4baf5d420796.jpg?1783940715"
        ruling("2012-05-01", "The spell you target could also target other things, as long as it also targets a player.")
    }
}
