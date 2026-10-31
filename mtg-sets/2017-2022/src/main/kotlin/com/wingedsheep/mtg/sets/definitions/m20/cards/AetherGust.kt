package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.TargetSpellOrPermanent

/**
 * Aether Gust — Core Set 2020 #42
 * {1}{U} · Instant · Uncommon
 *
 * Choose target spell or permanent that's red or green. Its owner puts it on their choice of the
 * top or bottom of their library.
 *
 * Sudden Setback's tuck with a colour restriction on both halves of the union target. The
 * restriction has to be passed to each filter separately, so the printed wording is supplied as
 * the description override rather than the generated "spell that's red or green or ..." text.
 * The top/bottom choice belongs to the owner and is made on resolution
 * ([Effects.PutOnTopOrBottomOfLibrary]); a spell answered this way leaves the stack without
 * being countered.
 */
val AetherGust = card("Aether Gust") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Choose target spell or permanent that's red or green. Its owner puts it on their " +
        "choice of the top or bottom of their library."

    spell {
        val t = target(
            TargetSpellOrPermanent(
                permanentFilter = GameObjectFilter.Permanent.withAnyColor(Color.RED, Color.GREEN),
                spellFilter = GameObjectFilter.Any.withAnyColor(Color.RED, Color.GREEN),
                descriptionOverride = "target spell or permanent that's red or green"
            )
        )
        effect = Effects.PutOnTopOrBottomOfLibrary(t)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "42"
        artist = "Tomasz Jedruszek"
        flavorText = "The chill wind extinguishes flame, and frosts the leaf on the branch."
        imageUri = "https://cards.scryfall.io/normal/front/7/8/783da808-6698-4e55-9fac-430a6effe2b1.jpg?1783933018"

        ruling(
            "2019-07-12",
            "The owner of the target spell or permanent chooses to put it either on the top or " +
                "bottom of their library."
        )
        ruling(
            "2019-07-12",
            "If a spell is put into its owner's library, it's removed from the stack and thus will " +
                "not resolve. The spell isn't countered; it no longer exists. This works against a " +
                "spell that can't be countered."
        )
    }
}
