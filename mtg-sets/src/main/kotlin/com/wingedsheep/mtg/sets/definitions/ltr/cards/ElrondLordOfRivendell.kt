package com.wingedsheep.mtg.sets.definitions.ltr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.IncrementAbilityResolutionCountEffect

/**
 * Elrond, Lord of Rivendell
 * {2}{U}
 * Legendary Creature — Elf Noble
 * 3/2
 *
 * Whenever Elrond or another creature you control enters, scry 1. If this is the second time this
 * ability has resolved this turn, the Ring tempts you.
 *
 * Composable: scry + `IncrementAbilityResolutionCountEffect` +
 * `ConditionalEffect(Conditions.SourceAbilityResolvedNTimes(2), TheRingTemptsYou())`. The increment
 * must run before the conditional reads the per-ability resolution count, or `count == 2` is never
 * met and the Ring never tempts (cf. Tannuk Memorial Ensign, Harvestrite Host).
 */
val ElrondLordOfRivendell = card("Elrond, Lord of Rivendell") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Elf Noble"
    power = 3
    toughness = 2
    oracleText = "Whenever Elrond or another creature you control enters, scry 1. If this is the second " +
        "time this ability has resolved this turn, the Ring tempts you."

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.youControl(),
            binding = TriggerBinding.ANY
        )
        effect = Patterns.Library.scry(1)
            .then(IncrementAbilityResolutionCountEffect)
            .then(
                ConditionalEffect(
                    condition = Conditions.SourceAbilityResolvedNTimes(2),
                    effect = Effects.TheRingTemptsYou()
                )
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "49"
        flavorText = "\"This is the hour of the Shire-folk, when they arise from their quiet fields to shake the towers and counsels of the Great. Who of all the Wise could have foreseen it?\""
        artist = "Ryan Yee"
        imageUri = "https://cards.scryfall.io/normal/front/a/6/a62a5e55-fa1b-4c70-9293-740dd513d52e.jpg?1686968084"
    }
}
