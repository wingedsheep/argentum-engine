package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AdditionalAttackTriggers
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Isshin, Two Heavens as One — Kamigawa: Neon Dynasty #224 (canonical printing)
 * {R}{W}{B} · Legendary Creature — Human Samurai · 3/4
 *
 * If a creature attacking causes a triggered ability of a permanent you control to trigger,
 * that ability triggers an additional time.
 *
 * The [AdditionalAttackTriggers] doubler (CR 603.2d) with no attacker restriction — the same
 * static Windcrag Siege's Mardu mode uses.
 */
val IsshinTwoHeavensAsOne = card("Isshin, Two Heavens as One") {
    manaCost = "{R}{W}{B}"
    colorIdentity = "RWB"
    typeLine = "Legendary Creature — Human Samurai"
    power = 3
    toughness = 4
    oracleText = "If a creature attacking causes a triggered ability of a permanent you control to trigger, " +
        "that ability triggers an additional time."

    staticAbility {
        ability = AdditionalAttackTriggers(attackerFilter = GameObjectFilter.Any)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "224"
        artist = "Ryan Pancoast"
        flavorText = "Trained by the Imperials but disillusioned by their rigidity, he gave his heart—and his " +
            "swords—to the Asari Uprising."
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a062a004-984e-4b62-960c-af7288f7a3e9.jpg?1783923833"
        ruling("2022-02-18", "Isshin, Two Heavens as One's ability affects only triggered abilities with conditions that are directly related to attacking, such as \"whenever [this creature] attacks\" or \"whenever you attack with one or more creatures.\" It does not affect triggered abilities with other trigger conditions, such as \"whenever [this creature] becomes tapped.\"")
        ruling("2022-02-18", "Isshin's effect doesn't copy the triggered ability; it just causes the ability to trigger twice. Any choices made as you put the ability onto the stack, such as modes and targets, are made separately for each instance of the ability. Any choices made on resolution are also made individually.")
    }
}
