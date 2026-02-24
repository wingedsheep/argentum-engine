package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jareth, Leonine Titan
 * {3}{W}{W}{W}
 * Legendary Creature — Cat Giant
 * 4/7
 * Whenever Jareth, Leonine Titan blocks, it gets +7/+7 until end of turn.
 * {W}: Jareth gains protection from the color of your choice until end of turn.
 */
val JarethLeonineTitan = card("Jareth, Leonine Titan") {
    manaCost = "{3}{W}{W}{W}"
    typeLine = "Legendary Creature — Cat Giant"
    power = 4
    toughness = 7
    oracleText = "Whenever Jareth, Leonine Titan blocks, it gets +7/+7 until end of turn.\n{W}: Jareth gains protection from the color of your choice until end of turn."

    triggeredAbility {
        trigger = Triggers.Blocks
        effect = Effects.ModifyStats(7, 7, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Mana("{W}")
        effect = Effects.ChooseColorAndGrantProtectionToTarget(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "43"
        artist = "Daren Bader"
        flavorText = "Light's champion in the stronghold of darkness."
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf57d9ec-d161-4cb4-989b-72278bfdba4c.jpg?1562936344"
    }
}
