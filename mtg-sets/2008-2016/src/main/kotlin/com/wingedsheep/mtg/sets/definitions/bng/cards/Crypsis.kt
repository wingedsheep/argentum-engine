package com.wingedsheep.mtg.sets.definitions.bng.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate

/**
 * Crypsis
 * {1}{U}
 * Instant
 * Target creature you control gains protection from creatures your opponents control until end
 * of turn. Untap it.
 *
 * Protection from creatures *your opponents control* — read relative to the protected creature's
 * controller each time it is checked — so it can't be blocked by, dealt damage by, or targeted by
 * abilities of creatures an opponent controls, while its controller's own creatures still match
 * nothing.
 */
val Crypsis = card("Crypsis") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Instant"
    oracleText = "Target creature you control gains protection from creatures your opponents control " +
        "until end of turn. Untap it."

    spell {
        val creature = target(TargetFilter.CreatureYouControl)
        effect = Effects.GrantProtectionFromCardType(
            CardType.CREATURE,
            creature,
            controller = ControllerPredicate.ControlledByOpponent
        ) then Effects.Untap(creature)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "34"
        artist = "Daarken"
        flavorText = "Tritons borrow the camouflage of their oceanic prey to become predators on land."
        imageUri = "https://cards.scryfall.io/normal/front/b/d/bd445b01-34af-4132-a9fe-a03858624492.jpg?1783939572"
    }
}
