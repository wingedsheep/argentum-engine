package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Incendiary Oracle
 * {1}{R}
 * Creature — Human Shaman
 * 2/2
 * {1}{R}: This creature gets +1/+0 until end of turn.
 * If a creature dealt damage by this creature this turn would die, exile it instead.
 *
 * The exile clause is the Frostwielder replacement: read at death time off this creature's per-turn
 * damaged-creature record, so it still applies when both die at once and stops applying once this
 * creature has left the battlefield (both rulings).
 */
val IncendiaryOracle = card("Incendiary Oracle") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman"
    power = 2
    toughness = 2
    oracleText = "{1}{R}: This creature gets +1/+0 until end of turn.\n" +
        "If a creature dealt damage by this creature this turn would die, exile it instead."

    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
        description = "{1}{R}: This creature gets +1/+0 until end of turn."
    }

    replacementEffect(
        RedirectZoneChange(
            newDestination = Zone.EXILE,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.wasDealtDamageBySourceThisTurn(),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD,
            ),
        )
    )

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "140"
        artist = "Cynthia Sheppard"
        flavorText = "The flamespeakers of Akros live passionately, devoted to Purphoros and the cycle of destructive creation."
        imageUri = "https://cards.scryfall.io/normal/front/e/b/eb6b9b5c-35ff-47ac-9b04-71257314b686.jpg?1783931550"
        ruling(
            "2020-01-24",
            "If a creature dealt damage by Incendiary Oracle would die at the same time as Incendiary Oracle, that creature is exiled instead."
        )
        ruling(
            "2020-01-24",
            "If a creature dealt damage by Incendiary Oracle would die after Incendiary Oracle has left the battlefield, that creature dies and isn't exiled instead."
        )
    }
}
