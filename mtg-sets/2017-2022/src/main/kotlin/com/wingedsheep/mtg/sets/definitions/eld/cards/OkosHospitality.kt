package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Oko's Hospitality — Throne of Eldraine #312 (canonical printing; Brawl deck)
 * {3}{G}{U} · Instant
 *
 * Creatures you control have base power and toughness 3/3 until end of turn. You may search your
 * library and/or graveyard for a card named Oko, the Trickster, reveal it, and put it into your
 * hand. If you search your library this way, shuffle.
 *
 * The base-P/T half is Oko, the Trickster's −7 shape: it iterates the creatures you control at
 * resolution, so creatures you gain later in the turn aren't 3/3. The search half is the
 * Liliana's Scorn planeswalker-deck tutor, and it still runs when you control no creatures (ruling).
 */
val OkosHospitality = card("Oko's Hospitality") {
    manaCost = "{3}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Instant"
    oracleText = "Creatures you control have base power and toughness 3/3 until end of turn. You may " +
        "search your library and/or graveyard for a card named Oko, the Trickster, reveal it, and put " +
        "it into your hand. If you search your library this way, shuffle."

    spell {
        effect = Effects.ForEachInGroup(
            GroupFilter.AllCreaturesYouControl,
            Effects.SetBasePowerAndToughness(3, 3, EffectTarget.IterationEntity, Duration.EndOfTurn)
        ) then Patterns.Library.searchLibraryAndOrGraveyard(
            filter = GameObjectFilter.Any.named("Oko, the Trickster"),
            destination = SearchDestination.HAND,
            reveal = true,
            optional = true
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "312"
        artist = "Deruchenko Alexander"
        flavorText = "\"My advisors recommend I let them eat you.\"\n—Oko"
        imageUri = "https://cards.scryfall.io/normal/front/8/1/81fb103d-f07c-4113-9da7-843cc7dab340.jpg?1783932553"
        ruling("2019-10-04", "You may cast Oko's Hospitality even if you control no creatures. You'll just search for Oko, the Trickster.")
        ruling("2019-10-04", "Oko's Hospitality overwrites all previous effects that set the creatures' base power and toughness to specific values. Any power- or toughness-setting effects that start to apply after Oko's Hospitality resolves will overwrite this effect.")
        ruling("2019-10-04", "Effects that modify a creature's power and/or toughness, such as the effect of Festive Funeral, will apply to the creature no matter when they started to take effect. The same is true for any counters that change its power and/or toughness.")
    }
}
