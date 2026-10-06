package com.wingedsheep.mtg.sets.definitions.m14.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedActivatedAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dragon Egg
 * {2}{R}
 * Creature — Dragon Egg
 * 0/2
 * Defender
 * When this creature dies, create a 2/2 red Dragon creature token with flying and
 * "{R}: This token gets +1/+0 until end of turn."
 */
val DragonEgg = card("Dragon Egg") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon Egg"
    power = 0
    toughness = 2
    oracleText = "Defender\nWhen this creature dies, create a 2/2 red Dragon creature token with flying " +
        "and \"{R}: This token gets +1/+0 until end of turn.\""

    keywords(Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Dragon"),
            keywords = setOf(Keyword.FLYING),
            activatedAbilities = listOf(
                grantedActivatedAbility {
                    cost = Costs.Mana("{R}")
                    effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
                    description = "{R}: This token gets +1/+0 until end of turn."
                },
            ),
            imageUri = "https://cards.scryfall.io/normal/front/0/e/0efaa5b5-984d-4eff-81b6-9b4989f149eb.jpg?1783939882",
        )
        description = "When this creature dies, create a 2/2 red Dragon creature token with flying " +
            "and \"{R}: This token gets +1/+0 until end of turn.\""
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "137"
        artist = "Jack Wang"
        flavorText = "Dragon birth lairs are littered with treasure to entice the young from their eggs."
        imageUri = "https://cards.scryfall.io/normal/front/d/c/dc2048f7-0c68-4142-9aad-de9b91fe5958.jpg?1783939914"
    }
}
