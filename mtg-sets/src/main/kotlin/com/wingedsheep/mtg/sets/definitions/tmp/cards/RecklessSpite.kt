package com.wingedsheep.mtg.sets.definitions.tmp.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Reckless Spite
 * {1}{B}{B}
 * Instant
 * Destroy two target nonblack creatures. You lose 5 life.
 *
 * Canonical printing is Tempest (earliest real expansion). Later printings
 * (Invasion, etc.) contribute only `Printing(...)` rows.
 */
val RecklessSpite = card("Reckless Spite") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Destroy two target nonblack creatures. You lose 5 life."

    spell {
        target = TargetCreature(count = 2, filter = TargetFilter.Creature.notColor(Color.BLACK))
        effect = Effects.Composite(
            listOf(
                Effects.Destroy(EffectTarget.ContextTarget(0)),
                Effects.Destroy(EffectTarget.ContextTarget(1)),
                Effects.LoseLife(5, EffectTarget.PlayerRef(Player.You))
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "152"
        artist = "Pete Venters"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/9141daea-1f4f-4227-b7d7-20753e3cb4d4.jpg?1562055421"
    }
}
