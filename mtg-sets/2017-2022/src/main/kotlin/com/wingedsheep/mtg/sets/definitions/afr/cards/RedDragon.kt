package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Red Dragon
 * {4}{R}{R}
 * Creature — Dragon
 * 4/4
 * Flying
 * Fire Breath — When this creature enters, it deals 4 damage to each opponent.
 */
val RedDragon = card("Red Dragon") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    oracleText = "Flying\nFire Breath — When this creature enters, it deals 4 damage to each opponent."
    power = 4
    toughness = 4
    keywords(Keyword.FLYING)
    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DealDamage(4, EffectTarget.PlayerRef(Player.EachOpponent))
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "160"
        artist = "Andrey Kuzinskiy"
        flavorText = "Red dragons consider the world and every creature in it as theirs to command."
        imageUri = "https://cards.scryfall.io/normal/front/7/c/7cf57369-b8e2-481c-89d4-c83c617499a3.jpg?1783926472"
    }
}
