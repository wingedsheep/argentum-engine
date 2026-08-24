package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CanOnlyBlockCreaturesWith
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Brassclaw Orcs
 * {2}{R}
 * Creature — Orc
 * 3/2
 * This creature can't block creatures with power 2 or greater.
 *
 * Expressed as its complement — the set of creatures it *can* block is exactly those with power
 * 1 or less, which is what `CanOnlyBlockCreaturesWith` takes. The comparison runs against
 * projected power, so a pumped attacker becomes unblockable by it mid-combat.
 */
val BrassclawOrcs = card("Brassclaw Orcs") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Orc"
    oracleText = "This creature can't block creatures with power 2 or greater."
    power = 3
    toughness = 2

    staticAbility {
        ability = CanOnlyBlockCreaturesWith(GameObjectFilter.Creature.powerAtMost(1))
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "49a"
        artist = "Rob Alexander"
        flavorText = "\"The Brassclaws delighted in lightning raids on Icatian and Dwarven towns; an unprepared enemy is easier to defeat.\"\n—*Sarpadian Empires, vol. IV*"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fc0cb8f6-6ba7-402c-9829-251f7443e871.jpg?1783947897"
    }
}
