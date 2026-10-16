package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Pip-Boy 3000
 * {1}
 * Artifact — Equipment
 * Whenever equipped creature attacks, choose one —
 * • Sort Inventory — Draw a card, then discard a card.
 * • Pick a Perk — Put a +1/+1 counter on that creature.
 * • Check Map — Untap up to two target lands.
 * Equip {2}
 *
 * "That creature" is the attacking creature the trigger fired for
 * ([EffectTarget.TriggeringEntity] under [Triggers.attached] — as on Ordeal of Nylea), not
 * whatever the Equipment happens to be attached to on resolution.
 */
val PipBoy3000 = card("Pip-Boy 3000") {
    manaCost = "{1}"
    colorIdentity = ""
    typeLine = "Artifact — Equipment"
    oracleText = "Whenever equipped creature attacks, choose one —\n" +
        "• Sort Inventory — Draw a card, then discard a card.\n" +
        "• Pick a Perk — Put a +1/+1 counter on that creature.\n" +
        "• Check Map — Untap up to two target lands.\n" +
        "Equip {2}"

    triggeredAbility {
        trigger = Triggers.attached.attacks()
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                Patterns.Hand.loot(draw = 1, discard = 1),
                "Sort Inventory — Draw a card, then discard a card",
            ),
            Mode.noTarget(
                Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.TriggeringEntity),
                "Pick a Perk — Put a +1/+1 counter on that creature",
            ),
            mode("Check Map — Untap up to two target lands") {
                targets(TargetFilter.Land, count = 2, optional = true)
                effect = Effects.ForEachTarget(Effects.Untap(EffectTarget.ContextTarget(0)))
            },
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "140"
        artist = "Mirko Failoni"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2248694-aab0-47a8-ae8b-8d5283075e10.jpg?1783912377"
    }
}
