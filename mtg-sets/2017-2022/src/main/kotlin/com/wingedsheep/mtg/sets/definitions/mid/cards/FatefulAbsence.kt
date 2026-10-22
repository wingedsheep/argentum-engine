package com.wingedsheep.mtg.sets.definitions.mid.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Fateful Absence
 * {1}{W}
 * Instant
 * Destroy target creature or planeswalker. Its controller investigates. (They create a Clue
 * token. It's an artifact with "{2}, Sacrifice this token: Draw a card.")
 *
 * "Its controller" is read after the destruction, so [EffectTarget.TargetController] falls back
 * to the permanent's last-known controller — the player who controlled it as it left.
 */
val FatefulAbsence = card("Fateful Absence") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Destroy target creature or planeswalker. Its controller investigates. (They create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"
    spell {
        val t = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.Destroy(t) then
            Effects.Investigate(controller = EffectTarget.TargetController)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "18"
        artist = "Eric Deschamps"
        flavorText = "Sensing a plot, Sorin raced to his grandfather's resting-crypt. But someone else had gotten there first."
        imageUri = "https://cards.scryfall.io/normal/front/e/c/eca8d6f8-c6f1-437c-99e2-4281eae14a6f.jpg?1783925659"
    }
}
