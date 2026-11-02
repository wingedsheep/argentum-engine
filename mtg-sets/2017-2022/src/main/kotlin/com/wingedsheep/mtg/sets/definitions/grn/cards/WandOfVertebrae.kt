package com.wingedsheep.mtg.sets.definitions.grn.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wand of Vertebrae
 * {1}
 * Artifact
 * {T}: Mill a card.
 * {2}, {T}, Exile this artifact: Shuffle up to five target cards from your graveyard into your library.
 *
 * Same shape as Perpetual Timepiece: the shuffle is unconditional so activating with zero
 * targets still shuffles your library.
 */
val WandOfVertebrae = card("Wand of Vertebrae") {
    manaCost = "{1}"
    typeLine = "Artifact"
    oracleText = "{T}: Mill a card.\n" +
        "{2}, {T}, Exile this artifact: Shuffle up to five target cards from your graveyard into your library."

    activatedAbility {
        cost = Costs.Tap
        effect = Patterns.Library.mill(1)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}"), Costs.Tap, Costs.ExileSelf)
        targets(TargetFilter(GameObjectFilter.Any.ownedByYou(), zone = Zone.GRAVEYARD), count = 5, optional = true)
        effect = Effects.ForEachTarget(
            Effects.Move(EffectTarget.ContextTarget(0), Zone.LIBRARY)
        ) then Effects.ShuffleLibrary()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "242"
        artist = "Volkan Baǵa"
        flavorText = "Made by the mad for the work of the wicked."
        imageUri = "https://cards.scryfall.io/normal/front/8/7/87f208bc-e4dc-4d3a-8906-dccde3cc251b.jpg?1783934105"
    }
}
