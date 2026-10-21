package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Grazilaxx, Illithid Scholar
 * {1}{U}{U}
 * Legendary Creature — Horror
 * 3/2
 * Whenever a creature you control becomes blocked, you may return it to its owner's hand.
 * Whenever one or more creatures you control deal combat damage to a player, draw a card.
 */
val GrazilaxxIllithidScholar = card("Grazilaxx, Illithid Scholar") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Horror"
    oracleText = "Whenever a creature you control becomes blocked, you may return it to its owner's hand.\n" +
        "Whenever one or more creatures you control deal combat damage to a player, draw a card."
    power = 3
    toughness = 2
    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl()).becomesBlocked()
        optional = true
        effect = Effects.ReturnToHand(EffectTarget.TriggeringEntity)
    }
    triggeredAbility {
        trigger = Triggers.oneOrMore(GameObjectFilter.Creature).dealCombatDamageToAPlayer()
        effect = Effects.DrawCards(1)
    }
    metadata {
        rarity = Rarity.RARE
        collectorNumber = "60"
        artist = "Alexander Mokhov"
        flavorText = "\"You studied hard. How delicious!\""
        imageUri = "https://cards.scryfall.io/normal/front/9/8/98cca453-4611-4f3d-85d8-d74d260c7598.jpg?1783926512"
    }
}
