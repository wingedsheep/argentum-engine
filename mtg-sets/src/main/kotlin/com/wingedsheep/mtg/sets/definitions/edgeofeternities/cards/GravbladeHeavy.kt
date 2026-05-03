package com.wingedsheep.mtg.sets.definitions.edgeofeternities.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantDynamicStatsEffect
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.StaticTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Gravblade Heavy
 * {3}{B}
 * Creature — Human Soldier
 * As long as you control an artifact, this creature gets +1/+0 and has deathtouch.
 */
val GravbladeHeavy = card("Gravblade Heavy") {
    manaCost = "{3}{B}"
    typeLine = "Creature — Human Soldier"
    power = 3
    toughness = 4
    oracleText = "As long as you control an artifact, this creature gets +1/+0 and has deathtouch."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantDynamicStatsEffect(
                target = StaticTarget.SourceCreature,
                powerBonus = DynamicAmount.Fixed(1),
                toughnessBonus = DynamicAmount.Fixed(0)
            ),
            condition = Conditions.ControlArtifact
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.DEATHTOUCH, StaticTarget.SourceCreature),
            condition = Conditions.ControlArtifact
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "102"
        artist = "Andrew Mar"
        flavorText = "It's difficult to dodge a weapon that commands gravity itself."
        imageUri = "https://cards.scryfall.io/normal/front/b/3/b3872341-d711-407b-85e4-46ccb99988e1.jpg?1752946969"
    }
}
