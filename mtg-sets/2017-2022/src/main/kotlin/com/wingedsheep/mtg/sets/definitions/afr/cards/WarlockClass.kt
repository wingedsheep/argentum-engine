package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Warlock Class {B}
 * Enchantment — Class
 *
 * (Gain the next level as a sorcery to add its ability.)
 * At the beginning of your end step, if a creature died this turn, each opponent loses 1 life.
 *
 * {1}{B}: Level 2
 * When this Class becomes level 2, look at the top three cards of your library. Put one of them
 * into your hand and the rest into your graveyard.
 *
 * {6}{B}: Level 3
 * At the beginning of your end step, each opponent loses life equal to the life they lost this
 * turn. (Damage causes loss of life.)
 *
 * Level 1 is Bulette's morbid end-step intervening-if (`Conditions.CreatureDiedThisTurn` counts
 * every creature that died this turn, whether or not the Class was around — ruling 2021-07-23).
 * Level 2 is Strategic Planning's look-three-keep-one. Level 3 iterates the opponents with
 * `ForEachPlayer`, which rebinds "you" to the visited opponent, so each one loses exactly the
 * life *they* lost this turn (`lifeLostThisTurn` reads life lost, never netted against gains).
 */
val WarlockClass = card("Warlock Class") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Class"
    oracleText = "(Gain the next level as a sorcery to add its ability.)\n" +
        "At the beginning of your end step, if a creature died this turn, each opponent loses 1 life.\n" +
        "{1}{B}: Level 2 — When this Class becomes level 2, look at the top three cards of your " +
        "library. Put one of them into your hand and the rest into your graveyard.\n" +
        "{6}{B}: Level 3 — At the beginning of your end step, each opponent loses life equal to the " +
        "life they lost this turn. (Damage causes loss of life.)"

    // Level 1: morbid drain of 1 from each opponent at your end step.
    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.CreatureDiedThisTurn
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "At the beginning of your end step, if a creature died this turn, each opponent loses 1 life."
    }

    // Level 2: look at the top three, keep one, bin the rest.
    classLevel(2, "{1}{B}") {
        triggeredAbility {
            trigger = Triggers.self.enters()
            effect = Patterns.Library.lookAtTopAndKeep(count = 3, keepCount = 1)
            description = "When this Class becomes level 2, look at the top three cards of your library. " +
                "Put one of them into your hand and the rest into your graveyard."
        }
    }

    // Level 3: each opponent loses life again equal to what they already lost this turn.
    classLevel(3, "{6}{B}") {
        triggeredAbility {
            trigger = Triggers.you.beginningOf(Step.END)
            effect = Effects.ForEachPlayer(
                Player.EachOpponent,
                Effects.LoseLife(DynamicAmounts.lifeLostThisTurn(Player.You), EffectTarget.Controller)
            )
            description = "At the beginning of your end step, each opponent loses life equal to the " +
                "life they lost this turn."
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "125"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7faf899-96b7-454e-b634-6684c2d72f26.jpg?1783926487"
        ruling("2021-07-23", "Warlock Class does not need to have been on the battlefield when the creature died for its first ability. For example, if a creature died in combat and you cast Warlock Class in your second main phase, the ability will trigger at the beginning of your end step.")
        ruling("2021-07-23", "Warlock Class's first ability will trigger only once during your end step, no matter how many creatures died this turn. However, if no creatures have died so far this turn as the end step begins, the ability won't trigger at all. It's not possible to cause a creature to die during the end step in time to have the ability trigger.")
        ruling("2021-07-23", "Gaining a level is a normal activated ability. It uses the stack and can be responded to.")
        ruling("2021-07-23", "Gaining a level won't remove abilities that a Class had at a previous level.")
        ruling("2021-07-23", "You can't activate the first level ability of a Class unless that Class is level 1. Similarly, you can't activate the second level ability of a Class unless that Class is level 2.")
        ruling("2021-07-23", "You can multiclass or even control multiple Class enchantments of the same class. Each Class permanent tracks its own level separately.")
    }
}
