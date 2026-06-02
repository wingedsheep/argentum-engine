package com.wingedsheep.mtg.sets.definitions.blb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantFreeCastTargetFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.events.CounterTypeFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.dsl.Effects

/**
 * Wishing Well {3}{U}
 * Artifact
 *
 * {T}: Put a coin counter on this artifact. When you do, you may cast target instant or
 * sorcery card with mana value equal to the number of coin counters on this artifact from
 * your graveyard without paying its mana cost. If that spell would be put into your
 * graveyard, exile it instead. Activate only as a sorcery.
 */
val WishingWell = card("Wishing Well") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Artifact"
    oracleText = "{T}: Put a coin counter on this artifact. When you do, you may cast target instant or sorcery card with mana value equal to the number of coin counters on this artifact from your graveyard without paying its mana cost. If that spell would be put into your graveyard, exile it instead. Activate only as a sorcery."

    activatedAbility {
        cost = Costs.Tap
        timing = TimingRule.SorcerySpeed

        effect = Effects.Composite(
            listOf(
                // Put a coin counter on this artifact
                AddCountersEffect("coin", 1, EffectTarget.Self),
                // Gather all instant/sorcery from your graveyard
                GatherCardsEffect(
                    source = CardSource.FromZone(
                        zone = Zone.GRAVEYARD,
                        player = Player.You,
                        filter = GameObjectFilter.InstantOrSorcery
                    ),
                    storeAs = "graveyardSpells"
                ),
                // Filter to those with MV = number of coin counters on this artifact
                FilterCollectionEffect(
                    from = "graveyardSpells",
                    filter = CollectionFilter.ManaValueEquals(
                        DynamicAmounts.countersOnSelf(CounterTypeFilter.Named("coin"))
                    ),
                    storeMatching = "matchingSpells"
                ),
                // Select up to 1 (representing the "you may cast target" choice)
                SelectFromCollectionEffect(
                    from = "matchingSpells",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    storeSelected = "selected",
                    storeRemainder = "remainder",
                    prompt = "Choose an instant or sorcery card to cast"
                ),
                // Move selected to exile
                MoveCollectionEffect(
                    from = "selected",
                    destination = CardDestination.ToZone(Zone.EXILE)
                ),
                // Grant free cast from exile + exile after resolve
                GrantFreeCastTargetFromExileEffect(
                    target = EffectTarget.PipelineTarget("selected", 0),
                    exileAfterResolve = true
                )
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "81"
        artist = "Steven Belledin"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/edeb20aa-b253-49b8-9947-c397a3a4002a.jpg?1721426330"
        ruling("2024-07-26", "You cast the instant or sorcery while the ability is resolving and still on the stack. You can't wait to cast it later in the turn. Timing restrictions based on the card's type are ignored.")
        ruling("2024-07-26", "If the spell you cast has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
