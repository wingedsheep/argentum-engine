package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fairgrounds Trumpeter — Kaladesh #155.
 * {2}{G} · Creature — Elephant · 2/2
 *
 * At the beginning of each end step, if a +1/+1 counter was put on a permanent under your control
 * this turn, put a +1/+1 counter on this creature.
 *
 * The intervening-if reads turn history keyed on the permanent's controller *as the counter was
 * placed* — whoever placed it, on any permanent, even one that has since left or lost the counter
 * (the card's ruling) — through `CounterPutOnPermanentYouControlledThisTurn`.
 */
val FairgroundsTrumpeter = card("Fairgrounds Trumpeter") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elephant"
    power = 2
    toughness = 2
    oracleText = "At the beginning of each end step, if a +1/+1 counter was put on a permanent under " +
        "your control this turn, put a +1/+1 counter on this creature."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END)
        interveningIf = Conditions.CounterPutOnPermanentYouControlledThisTurn(CounterType.PLUS_ONE_PLUS_ONE)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "155"
        artist = "Jesper Ejsing"
        flavorText = "The louder the elephant trumpeted, the more the crowd cheered. And the more the " +
            "crowd cheered . . ."
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65983338-8806-4386-94a6-4670eb853848.jpg?1783937178"
    }
}
