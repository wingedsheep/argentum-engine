package com.wingedsheep.mtg.sets.definitions.lci.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PermanentsEnterTapped

/**
 * Dauntless Dismantler
 * {1}{W}
 * Creature — Human Artificer
 * 1/4
 *
 * Artifacts your opponents control enter tapped.
 * {X}{X}{W}, Sacrifice this creature: Destroy each artifact with mana value X.
 *
 * - "Artifacts your opponents control enter tapped" is a global [PermanentsEnterTapped] runtime
 *   replacement whose `appliesTo` filter is scoped to artifacts controlled by opponents of the
 *   Dismantler's controller. The controller-relative `opponentControls()` predicate is resolved
 *   against the Dismantler's own controller at entry time — the same pattern used by
 *   Authority of the Consuls for creatures.
 * - The activated ability uses a double-X mana cost ({X}{X}{W}): both X symbols pay the same
 *   chosen X value, so X=1 costs three mana ({1}{1}{W}), X=2 costs five mana ({2}{2}{W}), etc.
 *   The destroy filter uses `manaValueEqualsX()` so exactly the artifacts at the
 *   paid X value are destroyed — across ALL controllers (oracle says "each artifact", not "each
 *   artifact an opponent controls"). The controller sacrifices this creature as part of the
 *   activation cost (CR 602.2b), so the Dismantler is gone before the effect resolves.
 */
val DauntlessDismantler = card("Dauntless Dismantler") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Artificer"
    power = 1
    toughness = 4
    oracleText = "Artifacts your opponents control enter tapped.\n" +
        "{X}{X}{W}, Sacrifice this creature: Destroy each artifact with mana value X."

    // Artifacts your opponents control enter tapped.
    replacementEffect(
        PermanentsEnterTapped(
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Artifact.opponentControls(),
                to = Zone.BATTLEFIELD,
            )
        )
    )

    // {X}{X}{W}, Sacrifice this creature: Destroy each artifact with mana value X.
    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{X}{X}{W}"), Costs.SacrificeSelf)
        effect = Effects.DestroyAll(
            filter = GameObjectFilter.Artifact.manaValueEqualsX()
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "8"
        artist = "Dibujante Nocturno"
        imageUri = "https://cards.scryfall.io/normal/front/3/d/3d771631-0aab-4f09-b9a6-49b6b2d8d2aa.jpg?1782694606"
    }
}
