package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Oriq Loremage — Strixhaven: School of Mages #80 (canonical printing)
 * {2}{B}{B} · Creature — Human Warlock · 3/3
 *
 * {T}: Search your library for a card, put it into your graveyard, then shuffle. If it's an
 * instant or sorcery card, put a +1/+1 counter on this creature.
 *
 * A search pipeline: gather your library (`search = true`), choose up to one card (a search
 * for "a card" may still fail to find), put it into the graveyard, shuffle, then a
 * [whenMatches] check on that same collection against instant-or-sorcery gates the counter on
 * the Loremage itself.
 */
val OriqLoremage = card("Oriq Loremage") {
    manaCost = "{2}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warlock"
    power = 3
    toughness = 3
    oracleText = "{T}: Search your library for a card, put it into your graveyard, then shuffle. " +
        "If it's an instant or sorcery card, put a +1/+1 counter on this creature."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.Pipeline {
            val library = gather(CardSource.FromZone(Zone.LIBRARY, Player.You), search = true)
            val found = chooseUpTo(1, from = library, prompt = "Search your library for a card to put into your graveyard")
            toGraveyard(found)
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
            run(
                Effects.If(
                    condition = whenMatches(found, GameObjectFilter.InstantOrSorcery),
                    then = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                )
            )
        }
        description = "Search your library for a card, put it into your graveyard, then shuffle. " +
            "If it's an instant or sorcery card, put a +1/+1 counter on this creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "80"
        artist = "JiHun Lee"
        flavorText = "Nothing can quell the Oriq's thirst for the forbidden secrets of the past."
        imageUri = "https://cards.scryfall.io/normal/front/2/5/25214a91-2f64-46ee-9834-7fbf684800f7.jpg?1783927363"
    }
}
