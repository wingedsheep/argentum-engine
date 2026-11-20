package com.wingedsheep.mtg.sets.definitions.bng.cards

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
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Felhide Spiritbinder
 * {3}{R}
 * Creature — Minotaur Shaman
 * 3/4
 *
 * Inspired — Whenever this creature becomes untapped, you may pay {1}{R}. If you do, create a token
 * that's a copy of another target creature, except it's an enchantment in addition to its other
 * types. It gains haste. Exile it at the beginning of the next end step.
 *
 * Saheeli Rai's −2 on an inspired trigger, behind Flameshadow Conjuring's `MayPay`: "except it's an
 * enchantment" is the copiable `addCardTypes`; "It gains haste" is a non-copy grant over
 * [CollectionSlot.CreatedTokens] (a creature that later copies the token doesn't get haste), and
 * the delayed exile rides `exileAtStep`, covering every token a replacement effect adds. The target
 * is chosen as the trigger goes on the stack; the {1}{R} is asked as it resolves.
 *
 * Canonical printing: Born of the Gods, the card's earliest real printing.
 */
val FelhideSpiritbinder = card("Felhide Spiritbinder") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Minotaur Shaman"
    power = 3
    toughness = 4
    oracleText = "Inspired — Whenever this creature becomes untapped, you may pay {1}{R}. If you do, " +
        "create a token that's a copy of another target creature, except it's an enchantment in addition " +
        "to its other types. It gains haste. Exile it at the beginning of the next end step."

    triggeredAbility {
        trigger = Triggers.self.becomesUntapped()
        val creature = target(TargetFilter(GameObjectFilter.Creature).other())
        effect = Effects.MayPay(
            ManaCost.parse("{1}{R}"),
            Effects.CreateTokenCopyOfTarget(
                target = creature,
                addCardTypes = setOf("ENCHANTMENT"),
                exileAtStep = Step.END,
            ) then Effects.ForEachInCollection(
                CollectionSlot.CreatedTokens,
                Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity, Duration.Permanent)
            )
        )
        description = "Inspired — Whenever this creature becomes untapped, you may pay {1}{R}. If you do, " +
            "create a token that's a copy of another target creature, except it's an enchantment in addition " +
            "to its other types. It gains haste. Exile it at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "96"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/b/b/bbbbe4ce-d6a6-428d-aeee-1c316f358777.jpg?1783939545"
        ruling("2014-02-01", "The token copies exactly what was printed on the original creature (except that the copy is also an enchantment) and nothing else (unless that permanent is copying something else or is a token). It doesn't copy whether that creature is tapped or untapped, whether it has any counters on it or Auras and Equipment attached to it, or any non-copy effects that have changed its power, toughness, types, color, and so on.")
        ruling("2014-02-01", "If another creature becomes or enters the battlefield as a copy of the token, that creature won't have haste and you won't exile it. However, if Felhide Spiritbinder creates multiple tokens due to a replacement effect (like the one Doubling Season creates), each of those tokens will have haste and you'll exile each of them.")
        ruling("2014-02-01", "If the inspired ability includes an optional cost, you decide whether to pay that cost as the ability resolves. You can do this even if the creature leaves the battlefield in response to the ability.")
    }
}
