package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Kodama's Reach
 * {2}{G}
 * Sorcery — Arcane
 * Search your library for up to two basic land cards, reveal those cards, put one onto the battlefield tapped and the other into your hand, then shuffle.
 *
 * The split needs two selections: pick up to two basics, then pick which found card enters
 * tapped — the remainder goes to hand. A single `Patterns.Library.searchLibrary(destination =
 * HAND, entersTapped = true)` sends *both* cards to hand. Same shape as Troop of Ponies.
 */
val KodamasReach = card("Kodama's Reach") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery — Arcane"
    oracleText = "Search your library for up to two basic land cards, reveal those cards, put one onto the battlefield tapped and the other into your hand, then shuffle."
    spell {
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.BasicLand),
                search = true
            )
            val found = chooseUpTo(
                2,
                from = searchable,
                prompt = "Search your library for up to two basic land cards"
            )
            val (toBattlefield, toHandCards) = chooseExactlySplit(
                1,
                from = found,
                selectedLabel = "Onto the battlefield tapped",
                remainderLabel = "Into your hand",
                prompt = "Choose which basic land enters the battlefield tapped; the other goes to your hand."
            )
            move(
                toBattlefield,
                CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped),
                revealed = true
            )
            toHand(toHandCards, revealed = true)
            run(Effects.ShuffleLibrary())
        }
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "225"
        artist = "Heather Hudson"
        flavorText = "\"The land grows only where the kami will it.\"\n—Dosan the Falling Leaf"
        imageUri = "https://cards.scryfall.io/normal/front/8/5/85d207ac-0680-47ef-85d9-4323c1321d6f.jpg"
    }
}
