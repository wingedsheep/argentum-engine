package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Stensia Innkeeper
 * {3}{R}
 * Creature — Vampire
 * 3/3
 *
 * When this creature enters, tap target land an opponent controls. That land doesn't untap during
 * its controller's next untap step.
 *
 * The Crippling Chill / Entangling Trap freeze on a land: both halves address the one target slot,
 * and [AbilityFlag.DOESNT_UNTAP] lasts [Duration.UntilAfterAffectedControllersNextUntap], expiring
 * with that land controller's next untap step rather than at end of turn.
 */
val StensiaInnkeeper = card("Stensia Innkeeper") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Vampire"
    power = 3
    toughness = 3
    oracleText = "When this creature enters, tap target land an opponent controls. That land " +
        "doesn't untap during its controller's next untap step."

    triggeredAbility {
        trigger = Triggers.self.enters()
        val land = target(TargetFilter.Land.opponentControls())
        effect = Effects.Tap(land) then
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                land,
                Duration.UntilAfterAffectedControllersNextUntap()
            )
        description = "tap target land an opponent controls. That land doesn't untap during its " +
            "controller's next untap step."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "145"
        artist = "Winona Nelson"
        flavorText = "\"The perils of the road, you say? Don't worry. You won't have to face those again.\""
        imageUri = "https://cards.scryfall.io/normal/front/e/e/ee40c471-70c8-4171-a214-ce932c4c7e2e.jpg?1783937454"
    }
}
