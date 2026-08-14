package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModeOption
import com.wingedsheep.sdk.scripting.conditions.SourceChosenModeIs
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gollum, Riddle Master
 * {1}{B}
 * Legendary Creature — Halfling Horror
 * 3/1
 *
 * As Gollum enters, choose odd or even. (Zero is even.)
 * Whenever an opponent casts a spell with mana value of the chosen quality, choose one that hasn't
 * been chosen —
 * • Put a +1/+1 counter on Gollum.
 * • Each opponent loses 2 life and you gain 2 life.
 * • Draw a card.
 *
 * The as-enters choice is an [EntersWithChoice] mode (the Khans/Dragons Siege shape), read back by
 * [SourceChosenModeIs]. There is no "chosen quality" indirection in the SDK — no predicate reads a
 * parity out of a stored choice slot — so the one printed trigger becomes **two mirrored triggers**,
 * each gated on the mode it belongs to and filtering on the matching parity. Exactly one of them can
 * ever be live on a given Gollum, so they can't both fire.
 *
 * `chooseOneNotYetChosen` (not the `ThisTurn` variant — Gollum's text has no "this turn") keeps the
 * chosen-mode memory on the permanent for as long as it stays the same object. Once all three modes
 * have been taken the ability still triggers but has no legal mode and does nothing; a Gollum that
 * leaves and returns is a new object and starts over.
 */
val GollumRiddleMaster = card("Gollum, Riddle Master") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Halfling Horror"
    oracleText = "As Gollum enters, choose odd or even. (Zero is even.)\n" +
        "Whenever an opponent casts a spell with mana value of the chosen quality, choose one " +
        "that hasn't been chosen —\n" +
        "• Put a +1/+1 counter on Gollum.\n" +
        "• Each opponent loses 2 life and you gain 2 life.\n" +
        "• Draw a card."
    power = 3
    toughness = 1

    replacementEffect(
        EntersWithChoice(
            choiceType = ChoiceType.MODE,
            modeOptions = listOf(
                ModeOption(
                    id = "odd",
                    label = "Odd",
                    description = "Trigger on opponents' spells with an odd mana value.",
                    iconKey = "odd",
                ),
                ModeOption(
                    id = "even",
                    label = "Even",
                    description = "Trigger on opponents' spells with an even mana value. (Zero is even.)",
                    iconKey = "even",
                ),
            )
        )
    )

    val riddle = ModalEffect.chooseOneNotYetChosen(
        Mode.noTarget(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            "Put a +1/+1 counter on Gollum",
        ),
        Mode.noTarget(
            Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent))
                .then(Effects.GainLife(2)),
            "Each opponent loses 2 life and you gain 2 life",
        ),
        Mode.noTarget(
            Effects.DrawCards(1),
            "Draw a card",
        ),
    )

    triggeredAbility {
        trigger = Triggers.opponentCasts(GameObjectFilter.Any.manaValueIsOdd())
        triggerCondition = SourceChosenModeIs("odd")
        effect = riddle
        description = "Whenever an opponent casts a spell with an odd mana value, choose one that " +
            "hasn't been chosen."
    }

    triggeredAbility {
        trigger = Triggers.opponentCasts(GameObjectFilter.Any.manaValueIsEven())
        triggerCondition = SourceChosenModeIs("even")
        effect = riddle
        description = "Whenever an opponent casts a spell with an even mana value, choose one " +
            "that hasn't been chosen."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "70"
        artist = "Irvin Rodriguez"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbdc7e37-c65a-497a-92b7-a30a6e369c71.jpg?1784376959"
    }
}
