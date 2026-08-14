package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastFromGraveyard

/**
 * Gisa and Geralf
 * {2}{U}{B}
 * Legendary Creature — Human Wizard
 * 4/4
 *
 * When Gisa and Geralf enters, mill four cards.
 * Once during each of your turns, you may cast a Zombie creature spell from your graveyard.
 *
 * The second line is a [MayCastFromGraveyard] grant, so the Zombie is cast for its normal cost
 * under normal timing rules (sorcery speed for a creature) — not a free cast. `oncePerTurn`
 * tracks the allowance on *this* permanent rather than on the player, which is what makes the
 * 2025-01-24 ruling work: a second Gisa and Geralf entering later in the same turn brings a
 * second use.
 */
val GisaAndGeralf = card("Gisa and Geralf") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Human Wizard"
    power = 4
    toughness = 4
    oracleText = "When Gisa and Geralf enters, mill four cards.\n" +
        "Once during each of your turns, you may cast a Zombie creature spell from your graveyard."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Library.mill(4)
        description = "When Gisa and Geralf enters, mill four cards."
    }

    staticAbility {
        ability = MayCastFromGraveyard(
            filter = GameObjectFilter.Creature.withSubtype(Subtype.ZOMBIE),
            duringYourTurnOnly = true,
            oncePerTurn = true,
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "183"
        artist = "Karla Ortiz"
        flavorText = "\"These fiends are slightly less tolerable than you.\"\n" +
            "\"A sentiment that warms my heart, sister.\""
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e2c85d8-0bef-40d0-abaf-4555462c29c5.jpg?1783937433"

        ruling("2025-01-24", "You must follow the normal timing permissions and restrictions of the Zombie spell you cast from your graveyard.")
        ruling("2025-01-24", "You must pay the costs to cast that Zombie spell. If it has an alternative cost, you may cast it for that cost instead.")
        ruling("2025-01-24", "Once you begin to cast the Zombie spell, losing control of Gisa and Geralf won't affect the spell.")
        ruling("2025-01-24", "If you cast one Zombie creature spell from your graveyard and then have a new Gisa and Geralf come under your control in the same turn, you may cast another Zombie creature spell from your graveyard that turn.")
        ruling("2025-01-24", "If multiple effects allow you to cast a Zombie creature spell from your graveyard, such as those of Gisa and Geralf and Karador, Ghost Chieftain, you must announce which permission you're using as you begin to cast the spell.")
    }
}
