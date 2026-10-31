package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Skophos Maze-Warden
 * {3}{R}
 * Creature — Minotaur Warrior
 * 3/4
 * {1}: This creature gets +1/-1 until end of turn.
 * Whenever another creature becomes the target of an ability of a land you control named
 * Labyrinth of Skophos, you may have this creature fight that creature.
 *
 * The trigger's `sourceFilter` reads the permanent whose ability did the targeting (the ability on
 * the stack has no card data of its own), so only Labyrinth of Skophos abilities you control count.
 */
val SkophosMazeWarden = card("Skophos Maze-Warden") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Minotaur Warrior"
    power = 3
    toughness = 4
    oracleText = "{1}: This creature gets +1/-1 until end of turn.\n" +
        "Whenever another creature becomes the target of an ability of a land you control named " +
        "Labyrinth of Skophos, you may have this creature fight that creature. " +
        "(Each deals damage equal to its power to the other.)"

    activatedAbility {
        cost = Costs.Mana("{1}")
        effect = Effects.ModifyStats(1, -1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature).becomesTarget(
            of = GameObjectFilter.Land.youControl().named("Labyrinth of Skophos"),
            abilitiesOnly = true,
        )
        optional = true
        effect = Effects.Fight(EffectTarget.Self, EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "153"
        artist = "Jason Rainville"
        imageUri = "https://cards.scryfall.io/normal/front/0/f/0f470a7b-63ef-4efa-855e-f8a3ce1ab534.jpg?1783931546"
        ruling(
            "2020-01-24",
            "An ability that triggers when a creature becomes the target of an ability resolves before the ability " +
                "that caused it to trigger. It resolves even if that ability is countered or if the creature is no " +
                "longer a legal target."
        )
        ruling(
            "2020-01-24",
            "If either Skophos Maze-Warden or the creature targeted by Labyrinth of Skophos has left the battlefield, " +
                "the other won't deal or be dealt damage."
        )
    }
}
