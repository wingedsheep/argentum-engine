package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange

/**
 * Frostwielder
 * {2}{R}{R}
 * Creature — Human Shaman
 * 1/2
 * {T}: This creature deals 1 damage to any target.
 * If a creature dealt damage by this creature this turn would die, exile it instead.
 */
val Frostwielder = card("Frostwielder") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman"
    oracleText = "{T}: This creature deals 1 damage to any target.\n" +
        "If a creature dealt damage by this creature this turn would die, exile it instead."
    power = 1
    toughness = 2

    activatedAbility {
        cost = Costs.Tap
        val victim = target(Targets.Any)
        effect = Effects.DealDamage(1, victim)
        description = "{T}: This creature deals 1 damage to any target."
    }

    // A printed replacement read at death time, not a damage-time mark: the rulings require this
    // creature to still be on the battlefield (or leaving simultaneously) when the damaged creature
    // would die.
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
        collectorNumber = "167"
        artist = "Christopher Moeller"
        imageUri = "https://cards.scryfall.io/normal/front/a/5/a54df527-6949-4b74-821b-b051f493f3c5.jpg?1783944300"
    }
}
