package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Torment of Venom
 * {2}{B}{B}
 * Instant
 *
 * Put three -1/-1 counters on target creature. Its controller loses 3 life unless they sacrifice
 * another nonland permanent of their choice or discard a card.
 *
 * "Another" is relative to the target creature, not the spell: state-based actions don't run
 * mid-resolution, so the creature is still on the battlefield while its controller is tormented,
 * and it may not be the sacrifice (an Aura on it may). `otherThan(creature)` names that exclusion.
 */
val TormentOfVenom = card("Torment of Venom") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Put three -1/-1 counters on target creature. Its controller loses 3 life unless they " +
        "sacrifice another nonland permanent of their choice or discard a card."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.AddCounters(CounterType.MINUS_ONE_MINUS_ONE, 3, creature) then
            Effects.PayOrSuffer(
                cost = Costs.pay.Choice(
                    listOf(
                        Costs.pay.Sacrifice(filter = GameObjectFilter.NonlandPermanent.otherThan(creature)),
                        Costs.pay.Discard()
                    )
                ),
                suffer = Effects.LoseLife(3, target = EffectTarget.TargetController),
                player = EffectTarget.TargetController,
                // Asked of the creature's controller, so the consequence is in their words.
                consequenceDescription = "lose 3 life"
            )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "79"
        artist = "Johann Bodin"
        flavorText = "The Scorpion God's sting sends venom strong enough to poison blood, bone, and spirit."
        imageUri = "https://cards.scryfall.io/normal/front/2/f/2f66410b-67a3-4f6a-b3ae-dcad9858a5da.jpg?1783936034"
    }
}
