package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kessig Flamebreather
 * {1}{R}
 * Creature — Human Shaman
 * 1/3
 * Whenever you cast a noncreature spell, this creature deals 1 damage to each opponent.
 */
val KessigFlamebreather = card("Kessig Flamebreather") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Shaman"
    oracleText = "Whenever you cast a noncreature spell, this creature deals 1 damage to each opponent."
    power = 1
    toughness = 3
    triggeredAbility {
        trigger = Triggers.YouCastNoncreature
        effect = DealDamageEffect(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }
    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "164"
        artist = "Lius Lasahido"
        flavorText = "\"Hunter's fire\" was meant for slaying monsters in the Ulvenwald, but that never stopped Ralen from showing it off."
        imageUri = "https://cards.scryfall.io/normal/front/3/0/303ad78a-b02a-44dc-afe6-7f95781a5062.jpg?1782703073"
    }
}
