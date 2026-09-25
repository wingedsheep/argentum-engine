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
 * Kumano, Master Yamabushi
 * {3}{R}{R}
 * Legendary Creature — Human Shaman
 * 4/4
 * {1}{R}: Kumano deals 1 damage to any target.
 * If a creature dealt damage by Kumano this turn would die, exile it instead.
 */
val KumanoMasterYamabushi = card("Kumano, Master Yamabushi") {
    manaCost = "{3}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Human Shaman"
    oracleText = "{1}{R}: Kumano deals 1 damage to any target.\n" +
        "If a creature dealt damage by Kumano this turn would die, exile it instead."
    power = 4
    toughness = 4

    activatedAbility {
        cost = Costs.Mana("{1}{R}")
        val victim = target(Targets.Any)
        effect = Effects.DealDamage(1, victim)
        description = "{1}{R}: Kumano deals 1 damage to any target."
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
        rarity = Rarity.RARE
        collectorNumber = "176"
        artist = "Adam Rex"
        imageUri = "https://cards.scryfall.io/normal/front/f/9/f93870f9-343f-43e7-8ae8-e7aaa56aacca.jpg?1783944299"
    }
}
