package com.wingedsheep.mtg.sets.definitions.ons.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CollectionFilter
import com.wingedsheep.sdk.scripting.effects.EachPlayerChoosesCreatureTypeEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.dsl.Effects

/**
 * Harsh Mercy
 * {2}{W}
 * Sorcery
 * Each player chooses a creature type. Destroy all creatures that aren't of a type
 * chosen this way. They can't be regenerated.
 */
val HarshMercy = card("Harsh Mercy") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Each player chooses a creature type. Destroy all creatures that aren't of a type chosen this way. They can't be regenerated."

    spell {
        effect = EachPlayerChoosesCreatureTypeEffect(storeAs = "chosenTypes")
            .then(Effects.Pipeline {
                val gathered = gather(
                    CardSource.BattlefieldMatching(filter = GameObjectFilter.Creature),
                    name = "destroyAll_gathered"
                )
                val filtered = filter(
                    gathered,
                    CollectionFilter.ExcludeSubtypesFromStored("chosenTypes"),
                    name = "destroyAll_filtered"
                )
                move(
                    filtered,
                    destination = CardDestination.ToZone(Zone.GRAVEYARD),
                    moveType = MoveType.Destroy,
                    noRegenerate = true
                )
            })
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "39"
        artist = "John Matson"
        flavorText = "\"There is no greater burden than choosing who to save.\" —Kamahl, druid acolyte"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b6473b4d-1f59-4216-ace9-f3e5306266fb.jpg?1562937932"
    }
}
