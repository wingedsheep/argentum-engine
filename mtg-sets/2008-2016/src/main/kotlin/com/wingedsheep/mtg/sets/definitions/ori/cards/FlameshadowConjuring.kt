package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Flameshadow Conjuring
 * {3}{R}
 * Enchantment
 * Whenever a nontoken creature you control enters, you may pay {R}. If you do, create a token
 * that's a copy of that creature. That token gains haste. Exile it at the beginning of the next
 * end step.
 *
 * The {R} is asked as the trigger resolves (`MayPay`). The copy reads the entering creature's
 * last-known copiable values when it has already left the battlefield.
 *
 * "That token gains haste" is a grant on the token, not a copy exception, so it is applied after
 * the copy with a permanent `GrantKeyword` (the Saheeli Rai shape) rather than `addedKeywords`:
 * a creature that later copies the token doesn't get haste (2015-06-22 ruling).
 * `exileAtStep = Step.END` fires on the next end step of any player's turn, whoever controls the
 * token then.
 */
val FlameshadowConjuring = card("Flameshadow Conjuring") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever a nontoken creature you control enters, you may pay {R}. If you do, create a " +
        "token that's a copy of that creature. That token gains haste. Exile it at the beginning of the " +
        "next end step."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.nontoken().youControl()).enters()
        effect = Effects.MayPay(
            ManaCost.parse("{R}"),
            Effects.CreateTokenCopyOfTarget(
                target = EffectTarget.TriggeringEntity,
                exileAtStep = Step.END,
            ) then Effects.ForEachInCollection(
                CollectionSlot.CreatedTokens,
                Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity, Duration.Permanent)
            )
        )
        description = "Whenever a nontoken creature you control enters, you may pay {R}. If you do, create a " +
            "token that's a copy of that creature. That token gains haste. Exile it at the beginning of the " +
            "next end step."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "147"
        artist = "Seb McKinnon"
        imageUri = "https://cards.scryfall.io/normal/front/b/4/b473ee0c-b037-4968-9dda-82c2288e54f7.jpg?1783938329"
        ruling(
            "2015-06-22",
            "The token copies exactly what's printed on the original creature and nothing else (unless that " +
                "creature is copying something else). It doesn't copy whether that creature is tapped or " +
                "untapped, whether it has any counters on it or Auras and Equipment attached to it, or any " +
                "non-copy effects that have changed its power, toughness, types, color, and so on."
        )
        ruling(
            "2015-06-22",
            "Any enters-the-battlefield abilities of the copied creature will trigger when the token enters " +
                "the battlefield. Any \"as [this permanent] enters the battlefield\" or \"[this permanent] enters " +
                "the battlefield with\" abilities of the copied creature will also work."
        )
        ruling(
            "2015-06-22",
            "The token is exiled at the beginning of the next end step regardless of who controls it at that time."
        )
        ruling(
            "2015-06-22",
            "If the ability resolves during a turn's end step, the token will be exiled at the beginning of the " +
                "next turn's end step."
        )
        ruling(
            "2015-06-22",
            "If the token isn't exiled at the beginning of the next end step (perhaps because the delayed " +
                "triggered ability is countered), it remains on the battlefield indefinitely. It continues to " +
                "have haste."
        )
        ruling(
            "2015-06-22",
            "If another creature becomes or enters the battlefield as a copy of the token, that creature won't " +
                "have haste and it won't be exiled."
        )
    }
}
