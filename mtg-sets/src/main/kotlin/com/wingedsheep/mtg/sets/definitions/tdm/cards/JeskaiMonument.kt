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
 * Jeskai Monument
 * {2}
 * Artifact
 *
 * When this artifact enters, search your library for a basic Island, Mountain, or Plains card,
 * reveal it, put it into your hand, then shuffle.
 * {1}{U}{R}{W}, {T}, Sacrifice this artifact: Create two 1/1 white Bird creature tokens with
 * flying. Activate only as a sorcery.
 *
 * The ETB search is a single (non-optional) library search restricted to basic Island /
 * Mountain / Plains cards via [GameObjectFilter.BasicLand.withAnyOfSubtypes]; it reveals the
 * found card, moves it to hand, then shuffles. The activated ability pays {1}{U}{R}{W} + tap +
 * sacrifice-self at sorcery speed ([TimingRule.SorcerySpeed]) and makes two flying Bird tokens.
 */
val JeskaiMonument = card("Jeskai Monument") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "When this artifact enters, search your library for a basic Island, Mountain, or Plains card, " +
        "reveal it, put it into your hand, then shuffle.\n" +
        "{1}{U}{R}{W}, {T}, Sacrifice this artifact: Create two 1/1 white Bird creature tokens with flying. " +
        "Activate only as a sorcery."

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = LibraryPatterns.searchLibrary(
            filter = GameObjectFilter.BasicLand.withAnyOfSubtypes(
                listOf(Subtype.ISLAND, Subtype.MOUNTAIN, Subtype.PLAINS)
            ),
            count = 1,
            destination = SearchDestination.HAND,
            shuffleAfter = true,
            reveal = true
        )
        description = "search your library for a basic Island, Mountain, or Plains card, reveal it, " +
            "put it into your hand, then shuffle."
    }

    activatedAbility {
        cost = AbilityCost.Composite(
            listOf(
                AbilityCost.Mana(ManaCost.parse("{1}{U}{R}{W}")),
                AbilityCost.Tap,
                AbilityCost.SacrificeSelf
            )
        )
        timing = TimingRule.SorcerySpeed
        effect = Effects.CreateToken(
            count = 2,
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Bird"),
            keywords = setOf(com.wingedsheep.sdk.core.Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/6/1/6105623a-ff2c-46bf-8881-e8b899d47d54.jpg?1742506584"
        )
        description = "Create two 1/1 white Bird creature tokens with flying. Activate only as a sorcery."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "244"
        artist = "Julian Kok Joon Wen"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0193ad6-39b7-4558-bd3e-36f809332ea2.jpg?1743204966"
    }
}
