package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PermanentsEnterTapped

/**
 * Spider-Woman, Stunning Savior
 * {1}{W/U}
 * Legendary Creature — Spider Human Hero
 * 2/2
 *
 * Flying
 * Venom Blast — Artifacts and creatures your opponents control enter tapped.
 *
 * - "Venom Blast" is a flavor ability word (CR 207.2c) with no rules meaning.
 * - The tap clause is the global [PermanentsEnterTapped] runtime replacement (the group counterpart
 *   of the self-only `EntersTapped`, as used by Authority of the Consuls), here with an
 *   `appliesTo` filter widened from creatures to *artifacts and creatures*. The
 *   `(Artifact or Creature)` union collapses to a single flat card-type predicate under the shared
 *   `opponentControls()` controller gate, which the enter-tap replacement resolver already
 *   understands; the controller-relative predicate is resolved against Spider-Woman's own
 *   controller at entry time.
 */
val SpiderWomanStunningSavior = card("Spider-Woman, Stunning Savior") {
    manaCost = "{1}{W/U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Spider Human Hero"
    oracleText = "Flying\n" +
        "Venom Blast — Artifacts and creatures your opponents control enter tapped."
    power = 2
    toughness = 2
    keywords(Keyword.FLYING)

    // Venom Blast — Artifacts and creatures your opponents control enter tapped.
    replacementEffect(
        PermanentsEnterTapped(
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = (GameObjectFilter.Artifact or GameObjectFilter.Creature).opponentControls(),
                to = Zone.BATTLEFIELD,
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "152"
        artist = "Justyna Dura"
        flavorText = "To know her is to fear her."
        imageUri = "https://cards.scryfall.io/normal/front/b/c/bc9b2a76-3cce-4fd0-a4ef-932747cb11b2.jpg?1783905310"
    }
}
