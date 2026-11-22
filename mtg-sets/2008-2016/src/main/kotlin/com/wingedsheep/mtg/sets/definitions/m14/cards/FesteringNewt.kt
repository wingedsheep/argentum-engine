package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Festering Newt
 * {B}
 * Creature — Salamander
 * 1/1
 * When this creature dies, target creature an opponent controls gets -1/-1 until end of turn.
 * That creature gets -4/-4 instead if you control a creature named Bogbrew Witch.
 *
 * The Bogbrew Witch check is made only as the trigger resolves (2017-11-17 ruling).
 */
val FesteringNewt = card("Festering Newt") {
    manaCost = "{B}"
    typeLine = "Creature — Salamander"
    power = 1
    toughness = 1
    oracleText = "When this creature dies, target creature an opponent controls gets -1/-1 until end of turn. " +
        "That creature gets -4/-4 instead if you control a creature named Bogbrew Witch."

    triggeredAbility {
        trigger = Triggers.self.dies()
        val creature = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.If(
            condition = Conditions.YouControl(GameObjectFilter.Creature.named("Bogbrew Witch")),
            then = Effects.ModifyStats(-4, -4, creature),
            otherwise = Effects.ModifyStats(-1, -1, creature),
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "98"
        artist = "Eric Deschamps"
        flavorText = "Its back bubbles like a witch's cauldron, and it smells just as vile."
        imageUri = "https://cards.scryfall.io/normal/front/e/a/eaee5261-416c-41e9-9ad7-bf7bd169aa08.jpg?1783939924"
        ruling(
            "2017-11-17",
            "Whether you control a creature named Bogbrew Witch is checked only as the triggered ability resolves. " +
                "How the target creature is affected won't change if you gain or lose control of all your Bogbrew Witches later in the turn."
        )
    }
}
