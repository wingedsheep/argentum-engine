package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword

/**
 * Omenport Vigilante
 * {1}{W}
 * Creature — Human Mercenary
 * 2/2
 * This creature has double strike as long as you've committed a crime this turn.
 *
 * The conditional keyword is a [ConditionalStaticAbility] granting double strike to itself
 * ([Filters.Self]) gated on [Conditions.YouCommittedCrimeThisTurn] — the crime-this-turn
 * tracker is read each time the projected state is computed, so double strike appears the
 * moment a crime is committed and disappears at end of turn when the tracker resets.
 */
val OmenportVigilante = card("Omenport Vigilante") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Mercenary"
    power = 2
    toughness = 2
    oracleText = "This creature has double strike as long as you've committed a crime this turn. " +
        "(Targeting opponents, anything they control, and/or cards in their graveyards is a crime.)"

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.DOUBLE_STRIKE, Filters.Self),
            condition = Conditions.YouCommittedCrimeThisTurn
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "21"
        artist = "Forrest Imel"
        flavorText = "Freestriders took to patrolling near the Omenpaths, protecting new arrivals " +
            "from the robbers and charlatans prowling for easy marks."
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7ecd8b6f-b9aa-466a-909c-3209beef8244.jpg?1712355310"

        ruling("2024-04-12", "A player commits a crime as they cast a spell, activate an ability, or put a triggered ability on the stack that targets at least one opponent, at least one permanent, spell, or ability an opponent controls, and/or at least one card in an opponent's graveyard.")
        ruling("2024-04-12", "Whether you've committed a crime this turn is checked continuously. Omenport Vigilante loses double strike at the start of the cleanup step, when \"this turn\" effects end.")
    }
}
