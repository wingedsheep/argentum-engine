package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersTapped
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode

/**
 * Federation Outpost
 * Land
 *
 * This land enters tapped unless you control two or more other lands.
 * When this land enters untapped, choose one —
 * • You gain 2 life.
 * • Surveil 1.
 * {T}: Add {W}.
 *
 * "Enters untapped" is the `untapped()` state predicate on the self enters trigger (as on
 * Idyllic Grange); the mode is chosen as the trigger goes on the stack.
 */
val FederationOutpost = card("Federation Outpost") {
    manaCost = ""
    colorIdentity = "W"
    typeLine = "Land"
    oracleText = "This land enters tapped unless you control two or more other lands.\n" +
        "When this land enters untapped, choose one —\n" +
        "• You gain 2 life.\n" +
        "• Surveil 1.\n" +
        "{T}: Add {W}."

    replacementEffect(EntersTapped(unlessCondition = Conditions.YouControlOtherAtLeast(2, GameObjectFilter.Land)))

    triggeredAbility {
        trigger = Triggers.self.matching(GameObjectFilter.Any.untapped()).enters()
        effect = ModalEffect.chooseOne(
            Mode.noTarget(Effects.GainLife(2), "You gain 2 life"),
            Mode.noTarget(Effects.Surveil(1), "Surveil 1"),
            countsAsModalSpell = false
        )
    }

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "282"
        artist = "Edouard"
        imageUri = "https://cards.scryfall.io/normal/front/0/2/02e368bd-ece8-4201-8d1a-4c5287d5cfee.jpg?1792632946"
    }
}
