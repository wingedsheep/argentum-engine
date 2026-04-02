package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.conditions.Exists
import com.wingedsheep.sdk.scripting.effects.AddCountersToCollectionEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.GameEvent.ZoneChangeEvent
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Wick, the Whorled Mind
 * {3}{B}
 * Legendary Creature — Rat Warlock
 * 2/4
 *
 * Whenever Wick or another Rat you control enters, create a 1/1 black Snail creature
 * token if you don't control a Snail. Otherwise, put a +1/+1 counter on a Snail you control.
 *
 * {U}{B}{R}, Sacrifice a Snail: Wick deals damage equal to the sacrificed creature's power
 * to each opponent. Then draw cards equal to the sacrificed creature's power.
 */
val WickTheWhorledMind = card("Wick, the Whorled Mind") {
    manaCost = "{3}{B}"
    typeLine = "Legendary Creature — Rat Warlock"
    power = 2
    toughness = 4
    oracleText = "Whenever Wick or another Rat you control enters, create a 1/1 black Snail creature token if you don't control a Snail. Otherwise, put a +1/+1 counter on a Snail you control.\n{U}{B}{R}, Sacrifice a Snail: Wick deals damage equal to the sacrificed creature's power to each opponent. Then draw cards equal to the sacrificed creature's power."

    // Triggered ability: Whenever a Rat you control enters (including self, since Wick is a Rat)
    triggeredAbility {
        trigger = TriggerSpec(
            event = ZoneChangeEvent(
                filter = GameObjectFilter.Creature.withSubtype("Rat").youControl(),
                to = Zone.BATTLEFIELD
            ),
            binding = TriggerBinding.ANY
        )
        effect = ConditionalEffect(
            condition = Exists(
                player = Player.You,
                zone = Zone.BATTLEFIELD,
                filter = GameObjectFilter.Creature.withSubtype("Snail"),
                negate = true
            ),
            effect = Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Snail"),
                imageUri = "https://cards.scryfall.io/normal/front/d/9/d9bb0a91-b73e-465b-8c0e-50fc28e66fda.jpg?1721425912"
            ),
            elseEffect = CompositeEffect(
                listOf(
                    GatherCardsEffect(
                        source = CardSource.BattlefieldMatching(
                            filter = GameObjectFilter.Creature.withSubtype("Snail"),
                            player = Player.You
                        ),
                        storeAs = "snails"
                    ),
                    SelectFromCollectionEffect(
                        from = "snails",
                        selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                        storeSelected = "chosen_snail",
                        useTargetingUI = true,
                        prompt = "Choose a Snail to put a +1/+1 counter on"
                    ),
                    AddCountersToCollectionEffect(
                        collectionName = "chosen_snail",
                        counterType = Counters.PLUS_ONE_PLUS_ONE,
                        count = 1
                    )
                )
            )
        )
    }

    // Activated ability: {U}{B}{R}, Sacrifice a Snail
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{U}{B}{R}"),
            Costs.Sacrifice(GameObjectFilter.Creature.withSubtype("Snail"))
        )
        effect = Effects.DealDamage(DynamicAmounts.sacrificedPower(), EffectTarget.PlayerRef(Player.EachOpponent))
            .then(Effects.DrawCards(DynamicAmounts.sacrificedPower()))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "120"
        artist = "Andrea Piparo"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29089810-d7fb-4abe-b729-bfabed6aed2b.jpg?1721426554"
        ruling("2024-07-26", "Use the power of the sacrificed Snail as it last existed on the battlefield to determine how much damage to deal and how many cards to draw.")
    }
}
