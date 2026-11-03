package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.IncrementAbilityResolutionCountEffect

/**
 * Jake Sisko, War Correspondent — Star Trek #205
 * {1}{G} · Legendary Creature — Human Citizen · 2/2
 *
 * Landfall — Whenever a land you control enters, you gain 1 life. If this is the second time this
 * ability has resolved this turn, investigate.
 *
 * Same resolution-count pattern as Rumor Gatherer / Omnath, Locus of Creation: bump the source's
 * per-turn resolution counter, gain 1 life every time, and investigate only on the second resolution.
 */
val JakeSiskoWarCorrespondent = card("Jake Sisko, War Correspondent") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Human Citizen"
    power = 2
    toughness = 2
    oracleText = "Landfall — Whenever a land you control enters, you gain 1 life. If this is the second " +
        "time this ability has resolved this turn, investigate. (Create a Clue token. It's an artifact " +
        "with \"{2}, Sacrifice this token: Draw a card.\")"

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = IncrementAbilityResolutionCountEffect then
            Effects.GainLife(1) then
            Effects.If(
                condition = Conditions.SourceAbilityResolvedNTimes(2),
                then = Effects.Investigate(),
            )
        description = "Landfall — Whenever a land you control enters, you gain 1 life. If this is the " +
            "second time this ability has resolved this turn, investigate."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "205"
        artist = "Ariel Orea"
        flavorText = "\"The line between courage and cowardice is a lot thinner than most people believe.\""
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e47c6b6-f99b-46be-98a1-92c98416aefe.jpg?1793013276"
    }
}
