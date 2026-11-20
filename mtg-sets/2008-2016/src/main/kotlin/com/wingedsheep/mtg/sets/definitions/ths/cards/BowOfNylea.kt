package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Bow of Nylea
 * {1}{G}{G}
 * Legendary Enchantment Artifact
 * Attacking creatures you control have deathtouch.
 * {1}{G}, {T}: Choose one —
 * • Put a +1/+1 counter on target creature.
 * • Bow of Nylea deals 2 damage to target creature with flying.
 * • You gain 3 life.
 * • Put up to four target cards from your graveyard on the bottom of your library in any order.
 *
 * The last mode is Gravepurge's shape inside a mode: one optional four-slot graveyard target,
 * then the mode's [CardSource.ChosenTargets] moved to the bottom of your library in the order
 * you choose.
 */
val BowOfNylea = card("Bow of Nylea") {
    manaCost = "{1}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Enchantment Artifact"
    oracleText = "Attacking creatures you control have deathtouch.\n" +
        "{1}{G}, {T}: Choose one —\n" +
        "• Put a +1/+1 counter on target creature.\n" +
        "• Bow of Nylea deals 2 damage to target creature with flying.\n" +
        "• You gain 3 life.\n" +
        "• Put up to four target cards from your graveyard on the bottom of your library in any order."

    staticAbility {
        ability = GrantKeyword(
            Keyword.DEATHTOUCH,
            GroupFilter(GameObjectFilter.Creature.attacking().youControl())
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{G}"), Costs.Tap)
        effect = ModalEffect.chooseOne(
            // • Put a +1/+1 counter on target creature.
            mode("Put a +1/+1 counter on target creature") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, creature)
            },
            // • Bow of Nylea deals 2 damage to target creature with flying.
            mode("Deal 2 damage to target creature with flying") {
                val flyer = target(TargetFilter.Creature.withKeyword(Keyword.FLYING))
                effect = Effects.DealDamage(2, flyer)
            },
            // • You gain 3 life.
            Mode.noTarget(Effects.GainLife(3), "You gain 3 life"),
            // • Put up to four target cards from your graveyard on the bottom of your library in any order.
            mode("Put up to four cards from your graveyard on the bottom of your library") {
                targets(
                    TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.GRAVEYARD),
                    count = 4,
                    optional = true,
                )
                effect = Effects.Pipeline {
                    val chosen = gather(CardSource.ChosenTargets)
                    toLibraryBottom(chosen, order = CardOrder.ControllerChooses)
                }
            }
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "153"
        artist = "Yeong-Hao Han"
        imageUri = "https://cards.scryfall.io/normal/front/8/9/89a2c408-e953-4386-abaf-4af85aba9620.jpg?1783939748"
    }
}
