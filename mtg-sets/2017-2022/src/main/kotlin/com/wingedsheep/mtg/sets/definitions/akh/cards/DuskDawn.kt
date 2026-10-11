package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Dusk // Dawn — Amonkhet #210
 *
 * Dusk {2}{W}{W} — Sorcery: Destroy all creatures with power 3 or greater.
 * Dawn {3}{W}{W} — Sorcery: Aftermath (Cast this spell only from your graveyard. Then exile it.)
 *   Return all creature cards with power 2 or less from your graveyard to your hand.
 *
 * Aftermath (CR 702.127) is [Keyword.AFTERMATH] on the Dawn face: the engine offers Dawn only from
 * its owner's graveyard, never Dusk from there, and exiles Dawn whenever it leaves the stack after a
 * graveyard cast.
 */
val DuskDawn = card("Dusk // Dawn") {
    layout = CardLayout.SPLIT
    colorIdentity = "W"

    face("Dusk") {
        manaCost = "{2}{W}{W}"
        typeLine = "Sorcery"
        oracleText = "Destroy all creatures with power 3 or greater."

        spell {
            effect = Effects.DestroyAll(filter = GameObjectFilter.Creature.powerAtLeast(3))
        }
    }

    face("Dawn") {
        manaCost = "{3}{W}{W}"
        typeLine = "Sorcery"
        oracleText = "Aftermath (Cast this spell only from your graveyard. Then exile it.)\n" +
            "Return all creature cards with power 2 or less from your graveyard to your hand."
        keywords(Keyword.AFTERMATH)

        spell {
            effect = Effects.Pipeline {
                val creatures = gather(
                    CardSource.FromZone(Zone.GRAVEYARD, filter = GameObjectFilter.Creature.powerAtMost(2))
                )
                toHand(creatures)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "210"
        artist = "Noah Bradley"
        imageUri = "https://cards.scryfall.io/normal/front/9/3/937dbc51-b589-4237-9fce-ea5c757f7c48.jpg?1783936459"
        ruling("2017-04-18", "If another effect allows you to cast a split card with aftermath from any zone other than a graveyard, you can't cast the half with aftermath.")
        ruling("2017-04-18", "If another effect allows you to cast a split card with aftermath from a graveyard, you may cast either half. If you cast the half that has aftermath, you'll exile the card if it would leave the stack.")
        ruling("2017-04-18", "A spell with aftermath cast from a graveyard will always be exiled afterward, whether it resolves, it's countered, or it leaves the stack in some other way.")
    }
}
