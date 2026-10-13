package com.wingedsheep.mtg.sets.definitions.big.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.SearchDestination

/**
 * Transmutation Font — {5} Artifact (The Big Score, mythic).
 *
 * "{T}: Create your choice of a Blood token, a Clue token, or a Food token.
 *  {3}, {T}, Sacrifice three artifact tokens with different names: Search your library for an
 *  artifact card, put it onto the battlefield, then shuffle. Activate only as a sorcery."
 *
 * Rulings:
 *  - Blood/Clue/Food are artifact subtypes; "with different names" compares the token card names
 *    ("Blood", "Clue", "Food", …).
 *  - The second ability can only be activated when you could cast a sorcery (sorcery speed).
 */
val TransmutationFont = card("Transmutation Font") {
    manaCost = "{5}"
    typeLine = "Artifact"
    oracleText = "{T}: Create your choice of a Blood token, a Clue token, or a Food token.\n" +
        "{3}, {T}, Sacrifice three artifact tokens with different names: Search your library for an " +
        "artifact card, put it onto the battlefield, then shuffle. Activate only as a sorcery."

    // {T}: Create your choice of a Blood token, a Clue token, or a Food token.
    activatedAbility {
        cost = Costs.Tap
        effect = Effects.ChooseAction(listOf(
            EffectChoice(
                label = "Create a Blood token",
                effect = Effects.CreateBlood(),
            ),
            EffectChoice(
                label = "Create a Clue token",
                effect = Effects.CreateClue(),
            ),
            EffectChoice(
                label = "Create a Food token",
                effect = Effects.CreateFood(),
            )
        ))
    }

    // {3}, {T}, Sacrifice three artifact tokens with different names:
    //   Search your library for an artifact card, put it onto the battlefield, then shuffle.
    //   Activate only as a sorcery.
    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}"),
            Costs.Tap,
            Costs.SacrificeMultiple(3, GameObjectFilter.Artifact.token(), distinctNames = true)
        )
        effect = Patterns.Library.searchLibrary(
            filter = GameObjectFilter.Artifact,
            count = 1,
            destination = SearchDestination.BATTLEFIELD,
            shuffleAfter = true
        )
        timing = TimingRule.SorcerySpeed
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "28"
        artist = "Mark Poole"
        imageUri = "https://cards.scryfall.io/normal/front/e/6/e6cfe673-d688-499a-882b-4fe5418739e3.jpg?1739804242"
    }
}
