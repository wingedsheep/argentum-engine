package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Dark Sphere
 * {0}
 * Artifact
 * {T}, Sacrifice this artifact: The next time a source of your choice would deal damage to you
 * this turn, prevent half that damage, rounded down.
 *
 * The Circle of Protection family's single-instance shield with the prevented amount halved:
 * `PreventHalfNextDamageFromChosenSource` installs the same chosen-source floating effect the
 * Circles use, flagged to prevent only half the instance rounded down. The unprevented half is
 * still dealt, and the shield is spent either way — against a 1-damage source it prevents nothing
 * and is gone. That "spent even when it prevents nothing" behaviour is why this reuses the
 * next-instance shield rather than a damage-reduction static.
 */
val DarkSphere = card("Dark Sphere") {
    manaCost = "{0}"
    typeLine = "Artifact"
    oracleText = "{T}, Sacrifice this artifact: The next time a source of your choice would deal " +
        "damage to you this turn, prevent half that damage, rounded down."

    activatedAbility {
        cost = Costs.Composite(Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.PreventHalfNextDamageFromChosenSource()
        description = "{T}, Sacrifice this artifact: The next time a source of your choice would " +
            "deal damage to you this turn, prevent half that damage, rounded down."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "100"
        artist = "Mark Tedin"
        flavorText = "\"I was struck senseless for a moment, but revived when the strange curiosity I carried fell to the ground, screaming like a dying animal.\"\n—Barl, Lord Ith"
        imageUri = "https://cards.scryfall.io/normal/front/7/2/72cfe9b9-677d-4ecb-83ab-67fb6481371d.jpg?1783947927"
    }
}
