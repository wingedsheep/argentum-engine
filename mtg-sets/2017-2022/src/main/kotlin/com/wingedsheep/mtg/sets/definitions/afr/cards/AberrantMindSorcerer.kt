package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Aberrant Mind Sorcerer
 * {4}{U}
 * Creature — Human Elf Shaman Sorcerer
 * 3/4
 * Psionic Spells — When this creature enters, choose target instant or sorcery card in your
 * graveyard, then roll a d20.
 * 1—9 | You may put that card on top of your library.
 * 10—20 | Return that card to your hand.
 *
 * The card is targeted as the trigger goes on the stack; the d20 is rolled on resolution, and the
 * results table is a pair of `ifThen` rows over the roll. Only the 1—9 row is optional.
 */
val AberrantMindSorcerer = card("Aberrant Mind Sorcerer") {
    manaCost = "{4}{U}"
    typeLine = "Creature — Human Elf Shaman Sorcerer"
    power = 3
    toughness = 4
    oracleText = "Psionic Spells — When this creature enters, choose target instant or sorcery card in your graveyard, then roll a d20.\n" +
        "1—9 | You may put that card on top of your library.\n" +
        "10—20 | Return that card to your hand."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val spellCard = target(TargetFilter.InstantOrSorceryInYourGraveyard)
        effect = Effects.Pipeline {
            val result = rollDiceResult(sides = 20)
            fun between(low: Int, high: Int) = Conditions.All(
                Conditions.CompareAmounts(result.amount, ComparisonOperator.GTE, low),
                Conditions.CompareAmounts(result.amount, ComparisonOperator.LTE, high),
            )
            ifThen(between(1, 9)) {
                run(
                    Effects.May(
                        Effects.PutOnTopOfLibrary(spellCard),
                        prompt = "Put that card on top of your library?",
                    )
                )
            }
            ifThen(between(10, 20)) {
                run(Effects.ReturnToHand(spellCard))
            }
        }
        description = "When this creature enters, choose target instant or sorcery card in your graveyard, " +
            "then roll a d20. 1—9: you may put that card on top of your library. 10—20: return that card " +
            "to your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "44"
        artist = "Lius Lasahido"
        imageUri = "https://cards.scryfall.io/normal/front/d/9/d9c6401a-e152-4acf-a2ca-75c484f296d5.jpg?1783926519"
        ruling("2021-07-23", "An effect that says \"choose a target, then roll a d20\" or similar still uses the normal process of putting an ability on the stack and resolving it. Choosing targets is part of putting the ability on the stack and rolling the d20 happens later, as the ability resolves.")
        ruling("2021-07-23", "The instruction to roll a die and the effect that occurs because of the result are all part of the same ability. Players do not get the chance to respond to the ability after knowing the result of the roll.")
    }
}
