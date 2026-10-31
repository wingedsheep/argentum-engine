package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chandra's Regulator
 * {1}{R}
 * Legendary Artifact
 * Whenever you activate a loyalty ability of a Chandra planeswalker, you may pay {1}. If you do,
 * copy that ability. You may choose new targets for the copy.
 * {1}, {T}, Discard a Mountain card or a red card: Draw a card.
 *
 * The trigger is Keral Keep Disciples' Chandra-scoped loyalty trigger; the payoff is Rings of
 * Brighthearth's "may pay, then copy the triggering ability" — [Effects.CopyTargetSpellOrAbility]
 * over [EffectTarget.TriggeringEntity], whose executor offers new targets (CR 707.10c). The copy
 * is not activated, so it neither moves loyalty counters nor re-triggers the Regulator.
 */
val ChandrasRegulator = card("Chandra's Regulator") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Artifact"
    oracleText = "Whenever you activate a loyalty ability of a Chandra planeswalker, you may pay {1}. " +
        "If you do, copy that ability. You may choose new targets for the copy.\n" +
        "{1}, {T}, Discard a Mountain card or a red card: Draw a card."

    triggeredAbility {
        trigger = Triggers.you.activatesAbility(
            of = GameObjectFilter.Planeswalker.withSubtype("Chandra").onBattlefield(),
            loyalty = true,
        )
        effect = Effects.MayPay(
            cost = Effects.PayMana("{1}"),
            then = Effects.CopyTargetSpellOrAbility(EffectTarget.TriggeringEntity),
            descriptionOverride = "Pay {1}? If you do, copy that ability. You may choose new " +
                "targets for the copy."
        )
        description = "Whenever you activate a loyalty ability of a Chandra planeswalker, you may " +
            "pay {1}. If you do, copy that ability. You may choose new targets for the copy."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.Tap,
            Costs.Discard(
                GameObjectFilter.Land.withSubtype(Subtype.MOUNTAIN) or GameObjectFilter.Any.withColor(Color.RED)
            ),
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "131"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f1b1d5c-f71e-4c68-8baf-c134e42ed6f2.jpg?1783932981"
        ruling("2019-07-12", "You can't pay {1} more than once for each time the triggered ability of Chandra's Regulator resolves.")
        ruling("2019-07-12", "Copying a loyalty ability doesn't add or remove loyalty counters from any object.")
        ruling("2019-07-12", "If the loyalty ability has −X in its cost, the copy uses the same value of X.")
        ruling("2019-07-12", "The triggered ability of Chandra's Regulator and the copy it creates both resolve before the loyalty ability that caused it to trigger. They resolve even if that loyalty ability is countered.")
        ruling("2019-07-12", "A Mountain card is a land card with the subtype Mountain, not any land card with a mana ability that produces red mana. Land cards are normally colorless, even if they produce red mana.")
    }
}
