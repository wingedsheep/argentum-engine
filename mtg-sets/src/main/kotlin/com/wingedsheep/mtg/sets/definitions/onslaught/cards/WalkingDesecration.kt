package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ChooseCreatureTypeMustAttackEffect

/**
 * Walking Desecration
 * {2}{B}
 * Creature — Zombie
 * 1/1
 * {B}, {T}: Creatures of the creature type of your choice attack this turn if able.
 */
val WalkingDesecration = card("Walking Desecration") {
    manaCost = "{2}{B}"
    typeLine = "Creature — Zombie"
    power = 1
    toughness = 1
    oracleText = "{B}, {T}: Creatures of the creature type of your choice attack this turn if able."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{B}"), Costs.Tap)
        effect = ChooseCreatureTypeMustAttackEffect
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "180"
        artist = "Daren Bader"
        flavorText = "\"Such sacrilege turns blinding grief into blinding rage.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/3/c39f3e91-571a-4990-b1e8-db2a5bac34af.jpg?1562941229"
    }
}
