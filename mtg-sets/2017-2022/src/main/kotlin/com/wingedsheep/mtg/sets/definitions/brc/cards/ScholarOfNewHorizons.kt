package com.wingedsheep.mtg.sets.definitions.brc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Scholar of New Horizons — The Brothers' War Commander #6
 * {1}{W} · Creature — Human Scout 1/1
 *
 * This creature enters with a +1/+1 counter on it.
 * {T}, Remove a counter from a permanent you control: Search your library for a Plains card and
 * reveal it. If an opponent controls more lands than you, you may put that card onto the
 * battlefield tapped. If you don't put the card onto the battlefield, put it into your hand. Then
 * shuffle.
 *
 * The cost removes one counter of any kind from any permanent you control (a typeless
 * `Costs.RemoveCounters`). The search reveals the found Plains, then offers it onto the battlefield
 * tapped through an up-to split whose size is one only while an opponent controls more lands than
 * you (checked on resolution) — zero otherwise, which skips the choice. Whatever isn't put onto the
 * battlefield goes to hand, so declining the offer (the ruling allows it) also puts it in hand.
 */
val ScholarOfNewHorizons = card("Scholar of New Horizons") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Scout"
    power = 1
    toughness = 1
    oracleText = "This creature enters with a +1/+1 counter on it.\n" +
        "{T}, Remove a counter from a permanent you control: Search your library for a Plains card " +
        "and reveal it. If an opponent controls more lands than you, you may put that card onto the " +
        "battlefield tapped. If you don't put the card onto the battlefield, put it into your hand. " +
        "Then shuffle."

    replacementEffect(EntersWithCounters(
        counterType = CounterType.PLUS_ONE_PLUS_ONE,
        count = 1,
        selfOnly = true
    ))

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.RemoveCounters(count = 1))
        effect = Effects.Pipeline {
            val library = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Land.withSubtype(Subtype.PLAINS)),
                search = true
            )
            val found = chooseUpTo(1, from = library, prompt = "Search your library for a Plains card")
            reveal(found)
            val (toBattlefield, toHandCards) = chooseUpToSplit(
                DynamicAmounts.conditional(Conditions.OpponentControlsMoreLands, 1, 0),
                from = found,
                selectedLabel = "Put onto the battlefield tapped",
                remainderLabel = "Put into your hand"
            )
            move(toBattlefield, CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped))
            toHand(toHandCards)
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
        }
        description = "Search your library for a Plains card and reveal it. If an opponent controls " +
            "more lands than you, you may put that card onto the battlefield tapped. If you don't put " +
            "the card onto the battlefield, put it into your hand. Then shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "6"
        artist = "Randy Vargas"
        imageUri = "https://cards.scryfall.io/normal/front/e/8/e83e50d0-7851-4608-8cfd-63c54d1a7544.jpg?1783919722"
        ruling("2022-10-14", "You may choose to put the card into your hand rather than onto the battlefield even if an opponent controls more lands than you.")
    }
}
