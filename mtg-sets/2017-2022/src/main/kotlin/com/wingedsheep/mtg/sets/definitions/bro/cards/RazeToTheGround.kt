package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Raze to the Ground — The Brothers' War #149 (common)
 * {2}{R} · Sorcery
 *
 * This spell can't be countered.
 * Destroy target artifact. If its mana value was 1 or less, draw a card.
 *
 * The mana-value check runs *before* the destroy: a target read is live-only, so checking after
 * destruction would read the card in the graveyard instead of the permanent as it last existed on
 * the battlefield. The draw doesn't depend on the destroy succeeding (an indestructible artifact
 * with mana value 1 or less still draws). If the target is illegal on resolution the whole spell
 * fizzles and nothing is drawn.
 */
val RazeToTheGround = card("Raze to the Ground") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "This spell can't be countered.\n" +
        "Destroy target artifact. If its mana value was 1 or less, draw a card."

    cantBeCountered = true

    spell {
        val artifact = target(TargetFilter.Artifact)
        effect = Effects.If(
            condition = Conditions.TargetSpellManaValueAtMost(DynamicAmounts.fixed(1), artifact),
            then = Effects.Destroy(artifact) then Effects.DrawCards(1),
            otherwise = Effects.Destroy(artifact)
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "149"
        artist = "Joshua Raphael"
        flavorText = "First one, then a second, then a third of the ivory towers fell to the invaders, " +
            "who moved in a circle around the city like an apocalyptic clock."
        imageUri = "https://cards.scryfall.io/normal/front/8/3/838b25d2-7615-4375-a5e6-3d762c9072a5.jpg?1783920061"
        ruling(
            "2022-10-14",
            "\"This spell can't be countered\" means it can't be countered even by the ward keyword " +
                "ability. The spell's controller may still pay the ward cost if it targets an artifact " +
                "with ward, but Raze to the Ground won't be countered if they don't."
        )
    }
}
