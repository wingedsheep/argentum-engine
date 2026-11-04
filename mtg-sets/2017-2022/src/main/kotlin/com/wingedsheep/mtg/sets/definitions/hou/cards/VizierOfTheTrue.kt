package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ExertAsItAttacks
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Vizier of the True
 * {3}{W} Creature — Human Cleric 3/2
 * You may exert this creature as it attacks. (It won't untap during your next untap step.)
 * Whenever you exert a creature, tap target creature an opponent controls.
 *
 * "Whenever you exert a creature" sees every exert of a creature you control — this one as it
 * attacks, another's attack exert, or a `Costs.Exert` payment.
 */
val VizierOfTheTrue = card("Vizier of the True") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Cleric"
    oracleText = "You may exert this creature as it attacks. (It won't untap during your next untap step.)\n" +
        "Whenever you exert a creature, tap target creature an opponent controls."
    power = 3
    toughness = 2

    staticAbility {
        ability = ExertAsItAttacks()
    }

    triggeredAbility {
        trigger = Triggers.you.exerts(GameObjectFilter.Creature)
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.Tap(creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "28"
        artist = "Ryan Alexander Lee"
        flavorText = "Outnumbered, but not outmatched."
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1800882-e886-48d5-ac85-0a8e5841dc9a.jpg?1783936055"
        ruling("2017-04-18", "Some cards have abilities that trigger whenever you exert any creature. These abilities trigger when you exert that creature or any other creature you control.")
        ruling("2017-04-18", "If a creature has a targeted triggered ability that triggers when you exert it, you can exert it even if there isn't a legal target for that triggered ability.")
    }
}
