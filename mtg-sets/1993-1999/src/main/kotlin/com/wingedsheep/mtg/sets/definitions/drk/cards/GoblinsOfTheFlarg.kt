package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Goblins of the Flarg
 * {R}
 * Creature — Goblin Warrior
 * 1/1
 * Mountainwalk
 * When you control a Dwarf, sacrifice this creature.
 *
 * The second line is a *state*-triggered ability (CR 603.8), not an event trigger: it watches for
 * the condition being true rather than for a Dwarf entering, so it also fires on a Dwarf you gain
 * control of, and re-fires if you lose and regain one. Same shape as Merchant Ship's
 * "When you control no Islands, sacrifice this creature."
 */
val GoblinsOfTheFlarg = card("Goblins of the Flarg") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Warrior"
    power = 1
    toughness = 1
    oracleText = "Mountainwalk (This creature can't be blocked as long as defending player " +
        "controls a Mountain.)\n" +
        "When you control a Dwarf, sacrifice this creature."

    keywords(Keyword.MOUNTAINWALK)

    stateTriggeredAbility {
        condition = Conditions.YouControl(GameObjectFilter.Creature.withSubtype("Dwarf"))
        effect = Effects.SacrificeTarget(EffectTarget.Self)
        description = "When you control a Dwarf, sacrifice this creature"
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "70"
        artist = "Tom Wänerstrand"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd333b18-b896-4ab8-9c46-eed4efdd94f2.jpg?1783947934"
    }
}
