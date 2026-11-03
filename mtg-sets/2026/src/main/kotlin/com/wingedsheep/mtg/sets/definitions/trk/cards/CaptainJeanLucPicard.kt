package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.EntryCounter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Captain Jean-Luc Picard
 * {2}{W}
 * Legendary Creature — Human Officer
 * 2/3
 *
 * Other creatures you control get +1/+1.
 * Whenever you cast a creature or Spacecraft spell, create a 1/1 red Officer creature token.
 * Tap seven untapped Officers you control: Search your library for a Spacecraft card, put it onto
 * the battlefield with seven charge counters on it, then shuffle.
 *
 * Picard himself is an Officer and may be one of the seven tapped (the cost is not {T}, so
 * summoning-sick Officers can pay it). The found Spacecraft *enters with* the seven charge counters
 * (`entryCounters`), so it is already stationed — a creature at its threshold — the moment it lands,
 * rather than receiving the counters afterwards.
 */
val CaptainJeanLucPicard = card("Captain Jean-Luc Picard") {
    manaCost = "{2}{W}"
    typeLine = "Legendary Creature — Human Officer"
    colorIdentity = "W"
    oracleText = "Other creatures you control get +1/+1.\n" +
        "Whenever you cast a creature or Spacecraft spell, create a 1/1 red Officer creature token.\n" +
        "Tap seven untapped Officers you control: Search your library for a Spacecraft card, put it onto " +
        "the battlefield with seven charge counters on it, then shuffle."
    power = 2
    toughness = 3

    staticAbility {
        ability = ModifyStats(1, 1, GroupFilter.OtherCreaturesYouControl)
    }

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.Creature or GameObjectFilter.Any.withSubtype("Spacecraft"))
        effect = Effects.CreateToken(
            power = 1, toughness = 1,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Officer"),
            imageUri = "https://cards.scryfall.io/normal/front/d/6/d676cbef-8e31-4ae6-8dfe-6b84160f71e0.jpg?1793363893"
        )
    }

    activatedAbility {
        cost = Costs.TapPermanents(7, GameObjectFilter.Any.withSubtype("Officer"))
        effect = Effects.Pipeline {
            val searchable = gather(
                CardSource.FromZone(Zone.LIBRARY, Player.You, GameObjectFilter.Any.withSubtype("Spacecraft")),
                search = true
            )
            val found = chooseUpTo(
                1,
                from = searchable,
                prompt = "Search your library for a Spacecraft card to put onto the battlefield with seven " +
                    "charge counters on it",
                selectedLabel = "Put onto the battlefield"
            )
            ifNotEmpty(found) {
                run(
                    Effects.Move(
                        found.asTarget,
                        Zone.BATTLEFIELD,
                        fromZone = Zone.LIBRARY,
                        entryCounters = listOf(EntryCounter(CounterType.CHARGE, count = DynamicAmounts.fixed(7))),
                    )
                )
            }
            run(Effects.ShuffleLibrary())
            run(EmitLibrarySearchedEventEffect)
        }
        description = "Tap seven untapped Officers you control: Search your library for a Spacecraft card, put " +
            "it onto the battlefield with seven charge counters on it, then shuffle."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "7"
        artist = "Anna Pavleeva"
        flavorText = "\"Engage!\""
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b7114271-eb05-41e2-ad96-a34f53492312.jpg?1792508148"
    }
}
