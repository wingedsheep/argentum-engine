package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Tolarian Kraken
 * {4}{U}{U}
 * Creature — Kraken
 * 4/6
 * Whenever you draw a card, you may pay {1}. When you do, you may tap or untap target creature.
 *
 * "You may pay {1}. When you do, …" is a reflexive triggered ability (CR 603.12): the payment is
 * an [Effects.MayPay] gate on the draw trigger's resolution, and the target is chosen only when
 * the reflexive ability goes on the stack. The tap-or-untap choice (and the second "you may") is
 * made as the reflexive ability resolves — the same idiom as Stonybrook Angler.
 */
val TolarianKraken = card("Tolarian Kraken") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Kraken"
    power = 4
    toughness = 6
    oracleText = "Whenever you draw a card, you may pay {1}. When you do, you may tap or untap target creature."

    triggeredAbility {
        trigger = Triggers.you.draws()
        effect = Effects.MayPay(
            cost = Effects.PayMana("{1}"),
            then = Effects.ReflexiveTrigger(
                action = Effects.Nothing,
                optional = false,
                descriptionOverride = "you may tap or untap target creature"
            ) {
                val creature = target(TargetFilter.Creature)
                effect = Effects.May(
                    Effects.Modal(
                        modes = listOf(
                            Mode.noTarget(Effects.Tap(creature), "Tap that creature"),
                            Mode.noTarget(Effects.Untap(creature), "Untap that creature")
                        ),
                        chooseCount = 1,
                        countsAsModalSpell = false
                    )
                )
            }
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "80"
        artist = "Svetlin Velinov"
        flavorText = "\"It's contemplative, resourceful, and original. Everything else aside, it's the perfect student.\"\n—Naban, dean of iteration"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2776694-6183-498d-9a38-e4c5c9e78179.jpg?1783930716"
        ruling("2020-06-23", "While resolving Tolarian Kraken's ability, you can't pay more than {1} to tap or untap more than one creature.")
        ruling("2020-06-23", "If a spell or ability causes you to put a card into your hand without specifically using the word \"draw,\" it's not a card drawn.")
    }
}
