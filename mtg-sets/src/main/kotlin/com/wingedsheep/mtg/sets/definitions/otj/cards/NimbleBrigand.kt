package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility

/**
 * Nimble Brigand
 * {2}{U}
 * Creature — Human Rogue
 * 1/3
 * This creature can't be blocked if you've committed a crime this turn.
 * Whenever this creature deals combat damage to a player, draw a card.
 *
 * The conditional evasion is a [ConditionalStaticAbility] wrapping [CantBeBlocked] (source-scoped)
 * gated on [Conditions.YouCommittedCrimeThisTurn]; the crime-this-turn tracker is read each time
 * the projected state is computed, so the creature becomes unblockable the moment you commit a
 * crime and reverts at end of turn when the tracker resets.
 */
val NimbleBrigand = card("Nimble Brigand") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Rogue"
    power = 1
    toughness = 3
    oracleText = "This creature can't be blocked if you've committed a crime this turn. " +
        "(Targeting opponents, anything they control, and/or cards in their graveyards is a crime.)\n" +
        "Whenever this creature deals combat damage to a player, draw a card."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = CantBeBlocked(),
            condition = Conditions.YouCommittedCrimeThisTurn
        )
    }

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "58"
        artist = "Kim Sokol"
        flavorText = "The hard part wasn't sneaking aboard. It was safely exiting with Slickshot flair."
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73c74d48-362d-4c3b-9ff7-39bdd19657a6.jpg?1712355461"

        ruling("2024-04-12", "A player commits a crime as they cast a spell, activate an ability, or put a triggered ability on the stack that targets at least one opponent, at least one permanent, spell, or ability an opponent controls, and/or at least one card in an opponent's graveyard.")
        ruling("2024-04-12", "Whether you've committed a crime is checked as blocks are declared. Committing a crime after blockers are declared won't make Nimble Brigand unblocked.")
    }
}
