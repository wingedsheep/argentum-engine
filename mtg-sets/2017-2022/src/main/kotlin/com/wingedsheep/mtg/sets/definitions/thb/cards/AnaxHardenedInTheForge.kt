package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

private const val SATYR_TOKEN_IMAGE =
    "https://cards.scryfall.io/normal/front/9/0/903e30f3-580e-4a14-989b-ae0632363407.jpg?1783931453"

private fun satyrs(count: Int) = Effects.CreateToken(
    power = 1,
    toughness = 1,
    colors = setOf(Color.RED),
    creatureTypes = setOf("Satyr"),
    count = count,
    staticAbilities = listOf(CantBlock()),
    imageUri = SATYR_TOKEN_IMAGE,
)

/** "Create a Satyr. If the creature had power 4 or greater, create two instead." */
private val anaxPayoff = Effects.If(
    condition = Conditions.CompareAmounts(DynamicAmounts.triggeringPower(), ComparisonOperator.GTE, 4),
    then = satyrs(2),
    otherwise = satyrs(1),
)

/**
 * Anax, Hardened in the Forge
 * {1}{R}{R}
 * Legendary Enchantment Creature — Demigod
 * * /3
 * Anax's power is equal to your devotion to red.
 * Whenever Anax or another nontoken creature you control dies, create a 1/1 red Satyr creature
 * token with "This token can't block." If the creature had power 4 or greater, create two of those
 * tokens instead.
 *
 * Power is a `dynamicPower` CDA over [DynamicAmounts.devotionTo]. The "Anax or another nontoken
 * creature" trigger is Pawn of Ulamog's split — a self-dies trigger (so a token copy of Anax still
 * triggers, per the rulings) plus an another-nontoken-creature-you-control trigger. The power check
 * reads the dead creature's last-known power through [DynamicAmounts.triggeringPower].
 */
val AnaxHardenedInTheForge = card("Anax, Hardened in the Forge") {
    manaCost = "{1}{R}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Enchantment Creature — Demigod"
    power = 0
    toughness = 3
    oracleText = "Anax's power is equal to your devotion to red. (Each {R} in the mana costs of " +
        "permanents you control counts toward your devotion to red.)\n" +
        "Whenever Anax or another nontoken creature you control dies, create a 1/1 red Satyr creature " +
        "token with \"This token can't block.\" If the creature had power 4 or greater, create two of " +
        "those tokens instead."

    dynamicPower(DynamicAmounts.devotionTo(Color.RED))

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = anaxPayoff
        description = "Whenever Anax or another nontoken creature you control dies, create a 1/1 red " +
            "Satyr creature token with \"This token can't block.\" If the creature had power 4 or " +
            "greater, create two of those tokens instead."
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl().nontoken()).dies()
        effect = anaxPayoff
        description = "Whenever Anax or another nontoken creature you control dies, create a 1/1 red " +
            "Satyr creature token with \"This token can't block.\" If the creature had power 4 or " +
            "greater, create two of those tokens instead."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "125"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a07285b4-77c5-4a38-8810-20d7e49593ec.jpg?1783931556"
        ruling("2020-01-24", "If Anax dies at the same time as another nontoken creature you control, its ability triggers for each of them.")
        ruling("2020-01-24", "To determine whether you create two tokens, check the power of the creature that died immediately before it died.")
        ruling("2020-01-24", "If you create a token that's a copy of Anax, its last ability triggers when it dies.")
    }
}
