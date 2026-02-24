package com.wingedsheep.mtg.sets.definitions.onslaught.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.TargetCreatureOrPlayer

/**
 * Words of War
 * {2}{R}
 * Enchantment
 * {1}: The next time you would draw a card this turn, this enchantment deals 2 damage to any target instead.
 */
val WordsOfWar = card("Words of War") {
    manaCost = "{2}{R}"
    typeLine = "Enchantment"
    oracleText = "{1}: The next time you would draw a card this turn, this enchantment deals 2 damage to any target instead."

    activatedAbility {
        cost = Costs.Mana("{1}")
        val t = target("target", TargetCreatureOrPlayer())
        effect = Effects.ReplaceNextDraw(Effects.DealDamage(2, t))
        promptOnDraw = true
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "244"
        artist = "Justin Sweet"
        flavorText = "Passions can't be shackled by laws or mastered with logic. The choice is freedom or death."
        imageUri = "https://cards.scryfall.io/normal/front/2/5/2593a6a6-dc21-4742-acb8-f7092931b1ce.jpg?1562903864"
    }
}
