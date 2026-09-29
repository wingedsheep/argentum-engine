package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Oracle of Tragedy — {1}{U}
 * Creature — Human Wizard 1/3 (uncommon, MOM #71)
 *
 * When this creature enters or dies, choose one —
 * • Draw a card, then discard a card.
 * • Shuffle up to four target cards with mana value 3 or greater from your graveyard into your library.
 *
 * "Enters or dies" is two triggers sharing one modal effect (as Wary Thespian / Sanguine Evangelist).
 * The shuffle mode is Renewing Touch's shape: gather the chosen targets, shuffle them into the library.
 */
private fun oracleOfTragedyModes() = ModalEffect.chooseOne(
    mode("Draw a card, then discard a card.") {
        effect = Patterns.Hand.loot()
    },
    mode("Shuffle up to four target cards with mana value 3 or greater from your graveyard into your library.") {
        targets(
            TargetFilter.CardInGraveyard.ownedByYou().manaValueAtLeast(3),
            count = 4,
            optional = true,
        )
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            move(chosen, CardDestination.ToZone(Zone.LIBRARY, Player.You, ZonePlacement.Shuffled))
        }
    }
)

val OracleOfTragedy = card("Oracle of Tragedy") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Wizard"
    power = 1
    toughness = 3
    oracleText = "When this creature enters or dies, choose one —\n" +
        "• Draw a card, then discard a card.\n" +
        "• Shuffle up to four target cards with mana value 3 or greater from your graveyard into your library."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = oracleOfTragedyModes()
    }

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = oracleOfTragedyModes()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "71"
        artist = "Pavel Kolomeyets"
        imageUri = "https://cards.scryfall.io/normal/front/e/1/e1d2aa39-b876-4136-8e16-5272a8083235.jpg?1783917029"
    }
}
