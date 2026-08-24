package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dwarven Armorer
 * {R}
 * Creature — Dwarf
 * 0/2
 * {R}, {T}, Discard a card: Put a +0/+1 counter or a +1/+0 counter on target creature.
 *
 * "Or" between two counter kinds is a choice made on resolution, so it is a modal *ability*
 * (`countsAsModalSpell = false`) over one shared target.
 */
val DwarvenArmorer = card("Dwarven Armorer") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dwarf"
    oracleText = "{R}, {T}, Discard a card: Put a +0/+1 counter or a +1/+0 counter on target creature."
    power = 0
    toughness = 2

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{R}"), Costs.Tap, Costs.DiscardCard)
        target = Targets.Creature
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                AddCountersEffect(Counters.PLUS_ZERO_PLUS_ONE, 1, EffectTarget.ContextTarget(0)),
                "Put a +0/+1 counter on it"
            ),
            Mode.noTarget(
                AddCountersEffect(Counters.PLUS_ONE_PLUS_ZERO, 1, EffectTarget.ContextTarget(0)),
                "Put a +1/+0 counter on it"
            ),
            countsAsModalSpell = false
        )
        description = "{R}, {T}, Discard a card: Put a +0/+1 counter or a +1/+0 counter on target creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "50"
        artist = "Bryon Wackwitz"
        flavorText = "\"The few remaining pieces from this period suggest the Dwarves eventually made weapons and armor out of everything, even children's toys.\"\n—*Sarpadian Empires, vol. IV*"
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d50bf06-97ab-4874-a484-9289f41dc98e.jpg?1783947896"
    }
}
