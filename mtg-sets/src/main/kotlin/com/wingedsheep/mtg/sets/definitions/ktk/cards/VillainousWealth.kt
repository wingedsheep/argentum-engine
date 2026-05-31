package com.wingedsheep.mtg.sets.definitions.ktk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CastAnyNumberFromCollectionWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.FilterCollectionEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Villainous Wealth
 * {X}{B}{G}{U}
 * Sorcery
 * Target opponent exiles the top X cards of their library. You may cast any number
 * of spells with mana value X or less from among them without paying their mana costs.
 */
val VillainousWealth = card("Villainous Wealth") {
    manaCost = "{X}{B}{G}{U}"
    colorIdentity = "UBG"
    typeLine = "Sorcery"
    oracleText = "Target opponent exiles the top X cards of their library. You may cast any number of spells with mana value X or less from among them without paying their mana costs."

    spell {
        target("opponent", Targets.Opponent)

        effect = CompositeEffect(
            listOf(
                // Exile top X cards from target opponent's library
                GatherCardsEffect(
                    source = CardSource.TopOfLibrary(DynamicAmount.XValue, player = Player.TargetOpponent),
                    storeAs = "exiled"
                ),
                MoveCollectionEffect(
                    from = "exiled",
                    destination = CardDestination.ToZone(Zone.EXILE, player = Player.TargetOpponent)
                ),
                // Filter to nonland cards (spells only) with mana value ≤ X
                FilterCollectionEffect(
                    from = "exiled",
                    filter = CollectionFilter.MatchesFilter(GameObjectFilter.Nonland),
                    storeMatching = "nonland"
                ),
                FilterCollectionEffect(
                    from = "nonland",
                    filter = CollectionFilter.ManaValueAtMost(DynamicAmount.XValue),
                    storeMatching = "castable"
                ),
                // Cast any number of them for free, during this spell's resolution (the
                // controller can't wait until later in the turn — see the 2014-09-20 ruling).
                CastAnyNumberFromCollectionWithoutPayingCostEffect("castable")
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "211"
        artist = "Erica Yang"
        flavorText = "\"Gold buys death. Death earns gold.\""
        imageUri = "https://cards.scryfall.io/normal/front/0/6/0610e69b-8b7c-40e4-bb10-28ee4411f861.jpg?1562782076"
        ruling("2014-09-20", "You choose which spells (if any) to cast as Villainous Wealth resolves. If you do, you do so as part of the resolution of Villainous Wealth. You can't wait to cast them later in the turn.")
        ruling("2014-09-20", "If you cast a spell \"without paying its mana cost,\" you can't choose to cast it for any alternative costs. You can, however, pay additional costs, such as kicker costs. If the spell has any mandatory additional costs, those must be paid to cast the spell.")
        ruling("2014-09-20", "If a spell you cast has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
        ruling("2014-09-20", "Any cards you don't cast this way will remain in exile.")
    }
}
