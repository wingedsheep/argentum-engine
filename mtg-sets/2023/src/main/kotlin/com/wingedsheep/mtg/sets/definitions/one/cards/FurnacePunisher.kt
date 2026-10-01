package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Furnace Punisher
 * {2}{R}
 * Creature — Phyrexian Warrior
 * Menace
 * At the beginning of each player's upkeep, this creature deals 2 damage to that player unless
 * they control two or more basic lands.
 *
 * "Unless" is not an intervening-if: the trigger always goes on the stack, and the basic-land
 * count of the player whose upkeep it is (`Player.TriggeringPlayer`) is checked on resolution.
 */
val FurnacePunisher = card("Furnace Punisher") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Phyrexian Warrior"
    power = 3
    toughness = 3
    oracleText = "Menace\nAt the beginning of each player's upkeep, this creature deals 2 damage to that player unless they control two or more basic lands."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.UPKEEP)
        effect = Effects.If(
            condition = Conditions.Not(
                Conditions.CompareAmounts(
                    DynamicAmounts.battlefield(Player.TriggeringPlayer, GameObjectFilter.BasicLand).count(),
                    ComparisonOperator.GTE,
                    2
                )
            ),
            then = Effects.DealDamage(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
        )
        description = "At the beginning of each player's upkeep, this creature deals 2 damage to that player unless they control two or more basic lands."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "132"
        artist = "Lauren K. Cannon"
        flavorText = "She marks her favorite creations with a searing brand, leaving all others to be consumed by the furnace."
        imageUri = "https://cards.scryfall.io/normal/front/6/5/65e5fc08-7a04-4bba-81ba-990889cae96a.jpg?1783918030"
    }
}
