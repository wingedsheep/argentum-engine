package com.wingedsheep.mtg.sets.definitions.bloomburrow.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AddCountersToCollectionEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.OptionalCostEffect
import com.wingedsheep.sdk.scripting.effects.SacrificeEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Scavenger's Talent {B}
 * Enchantment — Class
 *
 * (Gain the next level as a sorcery to add its ability.)
 *
 * Whenever one or more creatures you control die, create a Food token.
 * This ability triggers only once each turn.
 *
 * {1}{B}: Level 2
 * Whenever you sacrifice a permanent, target player mills two cards.
 *
 * {2}{B}: Level 3
 * At the beginning of your end step, you may sacrifice three other nonland
 * permanents. If you do, return a creature card from your graveyard to the
 * battlefield with a finality counter on it.
 */
val ScavengersTalent = card("Scavenger's Talent") {
    manaCost = "{B}"
    typeLine = "Enchantment — Class"
    oracleText = "Whenever one or more creatures you control die, create a Food token. " +
        "This ability triggers only once each turn.\n" +
        "{1}{B}: Level 2 — Whenever you sacrifice a permanent, target player mills two cards.\n" +
        "{2}{B}: Level 3 — At the beginning of your end step, you may sacrifice three other nonland " +
        "permanents. If you do, return a creature card from your graveyard to the battlefield " +
        "with a finality counter on it."

    // Level 1: Whenever one or more creatures you control die, create a Food token.
    // This ability triggers only once each turn.
    triggeredAbility {
        trigger = Triggers.YourCreatureDies
        oncePerTurn = true
        effect = Effects.CreateFood(1)
    }

    // Level 2: Whenever you sacrifice a permanent, target player mills two cards.
    // Note: Uses batching trigger (fires once per batch, not per individual permanent)
    classLevel(2, "{1}{B}") {
        triggeredAbility {
            trigger = Triggers.YouSacrificeOneOrMore()
            val player = target("target player", Targets.Player)
            effect = EffectPatterns.mill(2, player)
        }
    }

    // Level 3: At the beginning of your end step, you may sacrifice three other nonland
    // permanents. If you do, return a creature card from your graveyard to the battlefield
    // with a finality counter on it.
    classLevel(3, "{2}{B}") {
        triggeredAbility {
            trigger = Triggers.YourEndStep
            effect = OptionalCostEffect(
                cost = SacrificeEffect(
                    filter = GameObjectFilter.NonlandPermanent,
                    count = 3,
                    excludeSource = true
                ),
                ifPaid = CompositeEffect(
                    listOf(
                        GatherCardsEffect(
                            source = CardSource.FromZone(
                                Zone.GRAVEYARD,
                                Player.You,
                                GameObjectFilter.Creature
                            ),
                            storeAs = "eligible"
                        ),
                        SelectFromCollectionEffect(
                            from = "eligible",
                            selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                            storeSelected = "chosen",
                            prompt = "Choose a creature card to return to the battlefield"
                        ),
                        MoveCollectionEffect(
                            from = "chosen",
                            destination = CardDestination.ToZone(Zone.BATTLEFIELD)
                        ),
                        AddCountersToCollectionEffect("chosen", Counters.FINALITY, 1)
                    )
                )
            )
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "111"
        artist = "Chris Seaman"
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a52b7fe-87ae-425b-85fd-b24e6e0395f1.jpg?1721814345"
        ruling("2024-07-26", "The creature card you return from your graveyard with Scavenger's Talent's level 3 class ability may be one of the nonland permanents you sacrificed.")
        ruling("2024-07-26", "If you sacrifice a permanent as part of casting a spell or activating an ability, Scavenger's Talent's level 2 class ability will resolve before that spell or ability.")
        ruling("2024-07-26", "Scavenger's Talent's level 2 class ability will trigger when you sacrifice it. If you sacrifice other permanents at the same time, it triggers for them as well.")
    }
}
