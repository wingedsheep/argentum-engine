package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AbilityCost
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.dsl.LibraryPatterns

/**
 * Sultai Monument — Tarkir: Dragonstorm #247
 * {2} · Artifact
 *
 * When this artifact enters, search your library for a basic Swamp, Forest, or Island card,
 * reveal it, put it into your hand, then shuffle.
 * {2}{B}{G}{U}, {T}, Sacrifice this artifact: Create two 2/2 black Zombie Druid creature tokens.
 * Activate only as a sorcery.
 *
 * Mirrors the other clan Monuments (Abzan/Jeskai/Mardu/Temur): the ETB is a single
 * (non-optional) basic-land search across the three Sultai basic subtypes via
 * [GameObjectFilter.BasicLand.withAnyOfSubtypes], revealed to hand then shuffle
 * ([LibraryPatterns.searchLibrary]). The activated ability pays {2}{B}{G}{U} + tap +
 * sacrifice-self at sorcery speed ([TimingRule.SorcerySpeed]) and creates two fixed 2/2 black
 * Zombie Druid tokens ([Effects.CreateToken]).
 */
val SultaiMonument = card("Sultai Monument") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, search your library for a basic Swamp, Forest, or Island card, " +
        "reveal it, put it into your hand, then shuffle.\n" +
        "{2}{B}{G}{U}, {T}, Sacrifice this artifact: Create two 2/2 black Zombie Druid creature tokens. " +
        "Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LibraryPatterns.searchLibrary(
            filter = GameObjectFilter.BasicLand.withAnyOfSubtypes(
                listOf(Subtype.SWAMP, Subtype.FOREST, Subtype.ISLAND)
            ),
            count = 1,
            destination = SearchDestination.HAND,
            shuffleAfter = true,
            reveal = true
        )
        description = "When this artifact enters, search your library for a basic Swamp, Forest, or Island card, " +
            "reveal it, put it into your hand, then shuffle."
    }

    activatedAbility {
        cost = AbilityCost.Composite(
            listOf(
                AbilityCost.Mana(ManaCost.parse("{2}{B}{G}{U}")),
                AbilityCost.Tap,
                AbilityCost.SacrificeSelf
            )
        )
        timing = TimingRule.SorcerySpeed
        effect = Effects.CreateToken(
            count = 2,
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Zombie", "Druid"),
            imageUri = "https://cards.scryfall.io/normal/front/f/1/f10d5813-7818-43e8-b08d-4ed8c54d0366.jpg?1748452772"
        )
        description = "Create two 2/2 black Zombie Druid creature tokens. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "247"
        artist = "Julian Kok Joon Wen"
        imageUri = "https://cards.scryfall.io/normal/front/4/5/45308e0e-b515-49ac-9960-a24e898dd321.jpg?1743204975"
    }
}
