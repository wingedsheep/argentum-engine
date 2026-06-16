package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.MayPayManaEffect

/**
 * Marchesa, Dealer of Death
 * {U}{B}{R}
 * Legendary Creature — Human Rogue
 * 3/4
 *
 * Whenever you commit a crime, you may pay {1}. If you do, look at the top two cards of your
 * library. Put one of them into your hand and the other into your graveyard.
 *
 * "Look at the top two, keep one in hand, the other to graveyard" is the standard
 * [Patterns.Library.lookAtTopAndKeep] composition (gather top 2 → choose 1 → move kept to hand,
 * rest to graveyard). The optional {1} payment gates it via [MayPayManaEffect].
 */
val MarchesaDealerOfDeath = card("Marchesa, Dealer of Death") {
    manaCost = "{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Legendary Creature — Human Rogue"
    power = 3
    toughness = 4
    oracleText = "Whenever you commit a crime, you may pay {1}. If you do, look at the top two " +
        "cards of your library. Put one of them into your hand and the other into your graveyard. " +
        "(Targeting opponents, anything they control, and/or cards in their graveyards is a crime.)"

    triggeredAbility {
        trigger = Triggers.YouCommitCrime
        effect = MayPayManaEffect(
            cost = ManaCost.parse("{1}"),
            effect = Patterns.Library.lookAtTopAndKeep(count = 2, keepCount = 1)
        )
        description = "Whenever you commit a crime, you may pay {1}. If you do, look at the top " +
            "two cards of your library. Put one of them into your hand and the other into your " +
            "graveyard."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "220"
        artist = "Ryan Pancoast"
        flavorText = "\"You should have folded when you had the chance.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee29b59c-d57c-4a03-bae7-e9dfa57d6bb1.jpg?1712356159"

        ruling("2024-04-12", "A player commits a crime as they cast a spell, activate an ability, or put a triggered ability on the stack that targets at least one opponent, at least one permanent, spell, or ability an opponent controls, and/or at least one card in an opponent's graveyard.")
        ruling("2024-04-12", "A player can commit only one crime per spell or ability they control. Targeting multiple opponents, permanents, spells, abilities, and/or cards with the same spell or ability doesn't constitute committing multiple crimes.")
        ruling("2024-04-12", "You choose whether to pay {1} as Marchesa's triggered ability resolves.")
    }
}
