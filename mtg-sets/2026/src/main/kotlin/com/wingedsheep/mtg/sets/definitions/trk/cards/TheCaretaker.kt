package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.references.Player

/**
 * The Caretaker
 * {5}{U}
 * Legendary Creature — Elemental
 * 5/6
 *
 * Vigilance, ward {2}
 * At the beginning of each end step, if you faced a dilemma this turn, target opponent exiles cards
 * from the top of their library until they exile a nonland card. You may cast that card without
 * paying its mana cost. (You face a dilemma as you choose one or more modes for a spell or ability.)
 *
 * The intervening-if reads the per-turn dilemma tracker (`Conditions.YouFacedADilemmaThisTurn`).
 * The exile walk is Chaos Wand's `gatherUntilMatch(..., Player.TargetOpponent)` pipeline; the free
 * cast happens during resolution. Unlike Chaos Wand, nothing returns the exiled cards — they stay
 * in exile, as does the nonland card if it isn't cast.
 */
val TheCaretaker = card("The Caretaker") {
    manaCost = "{5}{U}"
    typeLine = "Legendary Creature — Elemental"
    colorIdentity = "U"
    oracleText = "Vigilance, ward {2}\nAt the beginning of each end step, if you faced a dilemma this turn, " +
        "target opponent exiles cards from the top of their library until they exile a nonland card. " +
        "You may cast that card without paying its mana cost. " +
        "(You face a dilemma as you choose one or more modes for a spell or ability.)"
    power = 5
    toughness = 6
    keywords(Keyword.VIGILANCE)
    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        interveningIf = Conditions.YouFacedADilemmaThisTurn()
        target(Targets.Opponent)
        effect = Effects.Pipeline {
            val (found, exiled) = gatherUntilMatch(GameObjectFilter.Nonland, player = Player.TargetOpponent)
            exile(exiled, Player.TargetOpponent)
            ifNotEmpty(found) {
                run(Effects.May(Effects.CastFromCollectionWithoutPayingCost(found)))
            }
        }
        description = "At the beginning of each end step, if you faced a dilemma this turn, target opponent " +
            "exiles cards from the top of their library until they exile a nonland card. " +
            "You may cast that card without paying its mana cost."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "47"
        artist = "Sam Kennedy"
        imageUri = "https://cards.scryfall.io/normal/front/e/c/ec6a1967-78fc-4ef4-9816-6d9a9dde61fb.jpg?1793538731"
    }
}
