package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Harried Dronesmith — Murders at Karlov Manor #131
 * {3}{R} · Creature — Human Artificer · 2/3
 *
 * At the beginning of combat on your turn, create a 1/1 colorless Thopter artifact creature token
 * with flying. It gains haste until end of turn. Sacrifice it at the beginning of your next end
 * step.
 *
 * The token is minted by the ordinary [Effects.CreateToken] with `sacrificeAtStep = Step.END`,
 * which arms a delayed trigger sacrificing that specific token at the next end step. Because the
 * ability triggers at the beginning of combat on *your* turn, "your next end step" is always this
 * turn's end step, so the plain END step needs no controller's-turn narrowing.
 *
 * Haste rides along as a printed token keyword rather than a separate until-end-of-turn grant:
 * the token is sacrificed at that same turn's end step, so it can never outlive the haste and the
 * two model identically (the Saheeli, the Sun's Brilliance / Molten Duplication convention).
 */
val HarriedDronesmith = card("Harried Dronesmith") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Artificer"
    power = 2
    toughness = 3
    oracleText = "At the beginning of combat on your turn, create a 1/1 colorless Thopter artifact " +
        "creature token with flying. It gains haste until end of turn. Sacrifice it at the " +
        "beginning of your next end step."

    triggeredAbility {
        trigger = Triggers.BeginCombat
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = emptySet(),
            creatureTypes = setOf("Thopter"),
            keywords = setOf(Keyword.FLYING, Keyword.HASTE),
            artifactToken = true,
            name = "Thopter",
            sacrificeAtStep = Step.END,
        )
        description = "At the beginning of combat on your turn, create a 1/1 colorless Thopter " +
            "artifact creature token with flying. It gains haste until end of turn. Sacrifice it " +
            "at the beginning of your next end step."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "131"
        artist = "Lindsey Look"
        flavorText = "After the eleventh consecutive thopter failed to return from Gruul territory, " +
            "she developed a disposable model."
        imageUri = "https://cards.scryfall.io/normal/front/3/6/36cb25ee-3c84-40a8-ba45-7e44893deecf.jpg?1783912879"
    }
}
