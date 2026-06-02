package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EntityNumericProperty
import com.wingedsheep.sdk.scripting.values.EntityReference
import com.wingedsheep.sdk.dsl.Effects

/**
 * Sidisi, Regent of the Mire — Tarkir: Dragonstorm #92
 * {1}{B} · Legendary Creature — Zombie Snake Warlock · 1/3
 *
 * {T}, Sacrifice a creature you control with mana value X other than Sidisi:
 * Return target creature card with mana value X plus 1 from your graveyard to the
 * battlefield. Activate only as a sorcery.
 *
 * The target's mana value is cost-linked: X is the mana value of the creature
 * sacrificed to pay the activation cost, and the returnable card must have mana value
 * X + 1. Rather than introduce a bespoke "MV == sacrificed cost-object's MV + 1"
 * predicate, this is expressed as a resolution-time chain over existing primitives:
 *   1. gather every creature card in your graveyard,
 *   2. keep only those whose mana value equals the sacrificed creature's MV + 1
 *      (read from the cost-sacrificed permanent's last-known snapshot via
 *      EntityReference.Sacrificed — Rule 112.7a / 608.2h),
 *   3. choose one (always prompts so the controller confirms the return, even
 *      with a single candidate; zero candidates resolves silently),
 *   4. return it to the battlefield.
 */
val SidisiRegentOfTheMire = card("Sidisi, Regent of the Mire") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Zombie Snake Warlock"
    power = 1
    toughness = 3
    oracleText = "{T}, Sacrifice a creature you control with mana value X other than Sidisi: " +
        "Return target creature card with mana value X plus 1 from your graveyard to the battlefield. " +
        "Activate only as a sorcery."

    activatedAbility {
        // {T}, Sacrifice a creature other than Sidisi. X is the sacrificed creature's mana value.
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeAnother(Filters.Creature))
        timing = TimingRule.SorcerySpeed
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Creature),
                    storeAs = "graveyardCreatures"
                ),
                // Keep creature cards whose mana value is the sacrificed creature's MV (X) + 1.
                FilterCollectionEffect(
                    from = "graveyardCreatures",
                    filter = CollectionFilter.ManaValueEquals(
                        DynamicAmount.Add(
                            DynamicAmount.EntityProperty(
                                EntityReference.Sacrificed(0),
                                EntityNumericProperty.ManaValue
                            ),
                            DynamicAmount.Fixed(1)
                        )
                    ),
                    storeMatching = "returnable"
                ),
                // alwaysPrompt so the controller sees and confirms the returned card even
                // when only one creature qualifies (zero candidates still resolves silently).
                SelectFromCollectionEffect(
                    from = "returnable",
                    selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                    storeSelected = "chosen",
                    alwaysPrompt = true
                ),
                MoveCollectionEffect(
                    from = "chosen",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD)
                )
            )
        )
        description = "{T}, Sacrifice a creature you control with mana value X other than Sidisi: " +
            "Return target creature card with mana value X plus 1 from your graveyard to the battlefield. " +
            "Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "92"
        artist = "Diana Franco"
        flavorText = "As the rebels breached Qarsi Palace, she realized her time in power was at its end."
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47374d23-662b-4ba7-a94f-37c9bc759cc6.jpg?1743204332"
        ruling("2025-04-04", "If a creature on the battlefield or a creature card in a graveyard has {X} in its mana cost, X is 0 for the purpose of determining its mana value.")
    }
}
