package com.wingedsheep.mtg.sets.definitions.eoe.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Scout for Survivors {2}{W}
 * Sorcery
 *
 * Return up to three target creature cards with total mana value 3 or less
 * from your graveyard to the battlefield. Put a +1/+1 counter on each of them.
 *
 * Implementation note: cards are chosen at resolution via a SelectFromCollection
 * pipeline rather than as cast-time targets, because the SDK has no multi-target
 * validator with a cross-target sum constraint. For graveyard targets this has
 * no observable consequence on any current card — hexproof/shroud don't apply
 * in the graveyard, no printed trigger fires from being targeted in the
 * graveyard, and no printed counterspell branches on a spell's target count
 * for graveyard targets. The visible difference is cosmetic: the spell sits on
 * the stack with no `targets` list until it resolves.
 */
val ScoutForSurvivors = card("Scout for Survivors") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Return up to three target creature cards with total mana value 3 or less from your graveyard to the battlefield. Put a +1/+1 counter on each of them."

    spell {
        effect = CompositeEffect(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromZone(
                        zone = Zone.GRAVEYARD,
                        player = Player.You,
                        filter = GameObjectFilter.Creature
                    ),
                    storeAs = "graveyardCreatures"
                ),
                SelectFromCollectionEffect(
                    from = "graveyardCreatures",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(3)),
                    restrictions = listOf(SelectionRestriction.TotalManaValueAtMost(3)),
                    storeSelected = "chosen",
                    prompt = "Choose up to three creature cards with total mana value 3 or less",
                    selectedLabel = "Return to the battlefield with a +1/+1 counter"
                ),
                MoveCollectionEffect(
                    from = "chosen",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD),
                    addCounterType = CounterType.PLUS_ONE_PLUS_ONE
                )
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "33"
        artist = "Greg Staples"
        flavorText = "\"We've received transmission! They're alive—just barely.\"\n—Sunstar Free Company Mission Control"
        imageUri = "https://cards.scryfall.io/normal/front/e/b/ebf3a6dd-a447-46f9-8b10-091ac8cbaa18.jpg?1752946680"
        ruling("2025-07-25", "If a card in your graveyard has {X} in its mana cost, X is 0 for the purpose of determining its mana value.")
    }
}
