package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Hivespine Wolverine
 * {3}{G}{G}
 * Creature — Elemental Wolverine
 * 5/4
 *
 * When this creature enters, choose one —
 * • Put a +1/+1 counter on target creature you control.
 * • This creature fights target creature token.
 * • Destroy target artifact or enchantment.
 */
val HivespineWolverine = card("Hivespine Wolverine") {
    manaCost = "{3}{G}{G}"
    typeLine = "Creature — Elemental Wolverine"
    oracleText = "When this creature enters, choose one —\n" +
        "• Put a +1/+1 counter on target creature you control.\n" +
        "• This creature fights target creature token.\n" +
        "• Destroy target artifact or enchantment."
    power = 5
    toughness = 4

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = ModalEffect.chooseOne(
            // Mode 1: Put a +1/+1 counter on target creature you control
            Mode.withTarget(
                Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.ContextTarget(0)),
                Targets.CreatureYouControl,
                "Put a +1/+1 counter on target creature you control"
            ),
            // Mode 2: This creature fights target creature token
            Mode.withTarget(
                Effects.Fight(EffectTarget.Self, EffectTarget.ContextTarget(0)),
                TargetCreature(
                    filter = TargetFilter(
                        GameObjectFilter(cardPredicates = listOf(CardPredicate.IsCreature, CardPredicate.IsToken))
                    )
                ),
                "This creature fights target creature token"
            ),
            // Mode 3: Destroy target artifact or enchantment
            Mode.withTarget(
                Effects.Destroy(EffectTarget.ContextTarget(0)),
                Targets.ArtifactOrEnchantment,
                "Destroy target artifact or enchantment"
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "177"
        artist = "Lars Grant-West"
        imageUri = "https://cards.scryfall.io/normal/front/8/2/821970a3-a291-4fe9-bb13-dfc54f9c3caf.jpg?1721426835"
    }
}
