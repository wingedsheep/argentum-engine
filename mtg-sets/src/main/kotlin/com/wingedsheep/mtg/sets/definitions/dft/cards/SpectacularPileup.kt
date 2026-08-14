package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Spectacular Pileup
 * {3}{W}{W}
 * Sorcery
 *
 * All creatures and Vehicles lose indestructible until end of turn, then destroy all creatures
 * and Vehicles.
 * Cycling {2}
 *
 * Two sequential halves, and the order is load-bearing: the first strips indestructible as a
 * layer-6 floating effect on every creature and Vehicle, and only then does the destroy run — so
 * projection is recomputed in between and the sweeper actually kills indestructible permanents.
 * This is *not* "destroy, ignoring indestructible": the removal is a real characteristic change, so
 * anything that gains indestructible later in the turn (after this resolves) keeps it.
 *
 * The destroy half uses [Effects.DestroyAll], the gather-then-destroy pipeline, so all permanents
 * are destroyed simultaneously (CR 701.7b) and dies-triggers see the whole board leave at once —
 * rather than a per-permanent loop, which would let an earlier death change what a later one sees.
 */
val SpectacularPileup = card("Spectacular Pileup") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "All creatures and Vehicles lose indestructible until end of turn, then destroy " +
        "all creatures and Vehicles.\n" +
        "Cycling {2} ({2}, Discard this card: Draw a card.)"

    spell {
        effect = Patterns.Group.removeKeywordFromAll(
            keyword = Keyword.INDESTRUCTIBLE,
            filter = GroupFilter(GameObjectFilter.CreatureOrVehicle),
        ).then(
            Effects.DestroyAll(GameObjectFilter.CreatureOrVehicle)
        )
    }

    keywordAbility(KeywordAbility.Cycling(ManaCost.parse("{2}")))

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "29"
        artist = "Zezhou Chen"
        flavorText = "The first four crashes were accidental. Then the Endriders joined the fun."
        imageUri = "https://cards.scryfall.io/normal/front/a/2/a24a6309-0e69-45f0-a9ff-44d4997e7e4d.jpg?1783907914"
    }
}
