package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Expand the Sphere
 * {3}{G}
 * Sorcery
 *
 * Look at the top six cards of your library. Put up to two land cards from among them onto the
 * battlefield tapped and the rest on the bottom of your library in a random order. If you put
 * fewer than two lands onto the battlefield this way, proliferate a number of times equal to the
 * difference.
 *
 * Pipeline: look at six, choose up to two lands (split keeps the remainder), lands to the
 * battlefield tapped, the rest to the bottom at random, then `Repeat(2 − chosen, Proliferate)`.
 * Each proliferate is its own choice (and its own "whenever you proliferate" event).
 */
val ExpandTheSphere = card("Expand the Sphere") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Look at the top six cards of your library. Put up to two land cards from among them onto " +
        "the battlefield tapped and the rest on the bottom of your library in a random order. If you put " +
        "fewer than two lands onto the battlefield this way, proliferate a number of times equal to the " +
        "difference. (Choose any number of permanents and/or players, then give each another counter of " +
        "each kind already there.)"

    spell {
        effect = Effects.Pipeline {
            val looked = gather(CardSource.TopOfLibrary(6))
            val (lands, rest) = chooseUpToSplit(
                2,
                from = looked,
                filter = GameObjectFilter.Land,
                prompt = "Choose up to two land cards to put onto the battlefield tapped",
                selectedLabel = "Put onto the battlefield tapped",
                remainderLabel = "Put on the bottom in a random order"
            )
            move(lands, CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped))
            toLibraryBottom(rest, order = CardOrder.Random)
            run(
                Effects.Repeat(
                    2 - lands.count,
                    Effects.Proliferate()
                )
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "168"
        artist = "Sergey Glushakov"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/572e174e-99f7-4b5e-8506-1833adddbf07.jpg?1783918016"
    }
}
