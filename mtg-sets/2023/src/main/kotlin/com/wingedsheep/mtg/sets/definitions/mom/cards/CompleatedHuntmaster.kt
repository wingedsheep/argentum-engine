package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Compleated Huntmaster
 * {2}{B}
 * Creature — Phyrexian Elf Warrior
 * 2/3
 *
 * {1}, {T}, Sacrifice another creature or artifact: Incubate 3. (Create an Incubator token with three
 * +1/+1 counters on it and "{2}: Transform this token." It transforms into a 0/0 Phyrexian artifact creature.)
 */
val CompleatedHuntmaster = card("Compleated Huntmaster") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Phyrexian Elf Warrior"
    oracleText = "{1}, {T}, Sacrifice another creature or artifact: Incubate 3. (Create an Incubator token with " +
        "three +1/+1 counters on it and \"{2}: Transform this token.\" It transforms into a 0/0 Phyrexian " +
        "artifact creature.)"
    power = 2
    toughness = 3

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.Tap,
            Costs.SacrificeAnother(GameObjectFilter.Creature or GameObjectFilter.Artifact)
        )
        effect = Effects.Incubate(3)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "96"
        artist = "Alex Brock"
        flavorText = "The elves of Lorwyn sought perfection. The Etched Host supplied it."
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4fc23af5-18dd-45f9-8d82-76b48aaf6d4d.jpg?1783917015"
    }
}
