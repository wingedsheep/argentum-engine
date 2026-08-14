package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetOpponent
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Haunt the Network — Aetherdrift #207
 * {3}{U}{B} · Sorcery
 *
 * Choose target opponent. Create two 1/1 colorless Thopter artifact creature tokens with flying.
 * Then the chosen player loses X life and you gain X life, where X is the number of artifacts you
 * control.
 *
 * Order matters and the oracle text spells it out: the tokens are created *first*, and they are
 * artifact creatures, so they count themselves toward X. On an otherwise empty board this drains
 * for 2, not 0. [Effects.Composite] executes its steps in sequence against the updated state, so
 * the [DynamicAmount.Count] over battlefield artifacts sees the two new Thopters.
 *
 * The drain is modelled as separate life-loss and life-gain steps rather than
 * [Effects.DrainLife]: the card gains a flat X, not "life equal to the life lost this way", so the
 * gain is unaffected by anything that changed how much life the opponent actually lost.
 */
val HauntTheNetwork = card("Haunt the Network") {
    manaCost = "{3}{U}{B}"
    colorIdentity = "UB"
    typeLine = "Sorcery"
    oracleText = "Choose target opponent. Create two 1/1 colorless Thopter artifact creature " +
        "tokens with flying. Then the chosen player loses X life and you gain X life, where X is " +
        "the number of artifacts you control."

    spell {
        val opponent = target("target opponent", TargetOpponent())
        val artifactsYouControl = DynamicAmount.Count(
            Player.You,
            Zone.BATTLEFIELD,
            GameObjectFilter.Artifact
        )
        effect = Effects.Composite(
            Effects.CreateToken(
                power = 1,
                toughness = 1,
                creatureTypes = setOf("Thopter"),
                keywords = setOf(Keyword.FLYING),
                count = 2,
                artifactToken = true,
                imageUri = "https://cards.scryfall.io/normal/front/d/3/d38fc294-ad86-441e-96fe-4ca286a11218.jpg?1783907677",
            ),
            Effects.LoseLife(artifactsYouControl, opponent),
            Effects.GainLife(artifactsYouControl),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "207"
        artist = "Jeff Carpenter"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/478d236c-9778-435a-ac21-bd0017a17d5b.jpg?1783907857"
    }
}
