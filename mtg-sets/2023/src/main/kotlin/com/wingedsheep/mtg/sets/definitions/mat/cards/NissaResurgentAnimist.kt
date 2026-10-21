package com.wingedsheep.mtg.sets.definitions.mat.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.IncrementAbilityResolutionCountEffect

/**
 * Nissa, Resurgent Animist — March of the Machine: The Aftermath #22
 * {2}{G} · Legendary Creature — Elf Scout · Mythic · 3/3
 *
 * Landfall — Whenever a land you control enters, add one mana of any color. Then if this is the
 * second time this ability has resolved this turn, reveal cards from the top of your library until
 * you reveal an Elf or Elemental card. Put that card into your hand and the rest on the bottom of
 * your library in a random order.
 *
 * The landfall trigger bumps the source's per-turn resolution counter and branches on the exact
 * count (cf. Omnath, Locus of Creation): only the second resolution reveals; every other
 * resolution just adds mana. The trigger uses the stack — it isn't a mana ability.
 */
val NissaResurgentAnimist = card("Nissa, Resurgent Animist") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Elf Scout"
    power = 3
    toughness = 3
    oracleText = "Landfall — Whenever a land you control enters, add one mana of any color. Then if this " +
        "is the second time this ability has resolved this turn, reveal cards from the top of your library " +
        "until you reveal an Elf or Elemental card. Put that card into your hand and the rest on the bottom " +
        "of your library in a random order."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Land.youControl()).enters()
        effect = IncrementAbilityResolutionCountEffect then
            Effects.AddAnyColorMana() then
            Effects.If(
                condition = Conditions.SourceAbilityResolvedNTimes(2),
                then = Patterns.Library.revealUntilMatchToHand(
                    filter = GameObjectFilter.Any.withAnySubtype("Elf", "Elemental"),
                ),
            )
        description = "Landfall — Whenever a land you control enters, add one mana of any color. Then if " +
            "this is the second time this ability has resolved this turn, reveal cards from the top of your " +
            "library until you reveal an Elf or Elemental card. Put that card into your hand and the rest on " +
            "the bottom of your library in a random order."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "22"
        artist = "Tuan Duong Chu"
        imageUri = "https://cards.scryfall.io/normal/front/2/4/248c76d3-b5cb-4582-be17-7cd1d0cb0f58.jpg?1783916520"
        ruling("2023-05-12", "Nissa's landfall ability isn't a mana ability, even though it causes you to add mana. It uses the stack and can be responded to.")
        ruling("2023-05-12", "Each time the landfall ability resolves in a turn after the second time, you'll just add mana.")
        ruling("2023-05-12", "If you reveal your entire library and don't reveal an Elf or Elemental card, you'll put all revealed cards back in a random order. This uses the same physical action as shuffling, although it isn't technically a \"shuffle.\"")
    }
}
