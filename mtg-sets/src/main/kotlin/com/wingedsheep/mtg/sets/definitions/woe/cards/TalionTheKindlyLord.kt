package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceSlot
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Talion, the Kindly Lord
 * {2}{U}{B}
 * Legendary Creature — Faerie Noble
 * Flying
 * As Talion enters, choose a number between 1 and 10.
 * Whenever an opponent casts a spell with mana value, power, or toughness equal to the chosen
 * number, that player loses 2 life and you draw a card.
 * 3/4
 *
 * The entry choice is the Shapeshifter machinery: a true *as-enters* replacement
 * ([EntersWithChoice] of [ChoiceType.NUMBER], CR 614.1c — chosen before the permanent is on the
 * battlefield, so there is no priority window where the number is unset), recorded durably under
 * [ChoiceSlot.CHOSEN_NUMBER] and read back with [DynamicAmount.CastChoice].
 *
 * "mana value, power, **or** toughness equal to the chosen number" is a disjunction of the three
 * new dynamic equality predicates. Each is the open-ended sibling of an existing fixed/`X` form
 * ([CardPredicate.ManaValueEquals] / [CardPredicate.PowerEquals] / [CardPredicate.ToughnessEquals]),
 * and each answers *false* for a characteristic the object doesn't have — so a noncreature spell is
 * judged on its mana value alone, exactly as printed. Per the rulings, an `{X}` spell's mana value
 * on the stack already includes the X it was cast for, and cost reductions never change it, so both
 * fall out of comparing against the stack object's mana value.
 */
val TalionTheKindlyLord = card("Talion, the Kindly Lord") {
    manaCost = "{2}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Faerie Noble"
    power = 3
    toughness = 4
    oracleText = "Flying\n" +
        "As Talion enters, choose a number between 1 and 10.\n" +
        "Whenever an opponent casts a spell with mana value, power, or toughness equal to the " +
        "chosen number, that player loses 2 life and you draw a card."

    keywords(Keyword.FLYING)

    replacementEffect(
        EntersWithChoice(choiceType = ChoiceType.NUMBER, minValue = 1, maxValue = 10)
    )

    triggeredAbility {
        trigger = Triggers.opponentCasts(
            GameObjectFilter.Any.copy(
                cardPredicates = listOf(
                    CardPredicate.Or(
                        listOf(
                            CardPredicate.ManaValueEqualsDynamic(
                                DynamicAmount.CastChoice(ChoiceSlot.CHOSEN_NUMBER)
                            ),
                            CardPredicate.PowerEqualsDynamic(
                                DynamicAmount.CastChoice(ChoiceSlot.CHOSEN_NUMBER)
                            ),
                            CardPredicate.ToughnessEqualsDynamic(
                                DynamicAmount.CastChoice(ChoiceSlot.CHOSEN_NUMBER)
                            )
                        )
                    )
                )
            )
        )
        effect = Effects.Composite(
            Effects.LoseLife(2, EffectTarget.PlayerRef(Player.TriggeringPlayer)),
            Effects.DrawCards(1)
        )
        description = "Whenever an opponent casts a spell with mana value, power, or toughness " +
            "equal to the chosen number, that player loses 2 life and you draw a card."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "215"
        artist = "Justyna Dura"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62a6b452-c796-45c6-b4d1-0ae3d675e38e.jpg?1783915070"

        ruling("2023-09-01", "The numbers you may choose include 1 and 10.")
        ruling(
            "2023-09-01",
            "Effects that increase or reduce the cost to cast a spell don't affect that spell's " +
                "mana value."
        )
        ruling(
            "2023-09-01",
            "For spells with {X} in their mana costs, use the value chosen for X to determine if " +
                "the spell's mana value is the chosen number. For example, if the chosen number " +
                "is 4, a spell with mana cost {X}{R}{R} cast with X equal to 2 would cause " +
                "Talion's last ability to trigger."
        )
    }
}
