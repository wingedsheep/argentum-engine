package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Fate Foretold
 * {1}{U}
 * Enchantment — Aura
 *
 * Enchant creature
 * When this Aura enters, draw a card.
 * When enchanted creature dies, its controller draws a card.
 *
 * "Its controller" is the dead creature's last-known controller (CR 608.2h), not the Aura's
 * controller: [EffectTarget.ControllerOfTriggeringEntity] reads it off the creature's last-known
 * snapshot once it has left the battlefield.
 */
val FateForetold = card("Fate Foretold") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "When this Aura enters, draw a card.\n" +
        "When enchanted creature dies, its controller draws a card."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.DrawCards(1)
    }

    triggeredAbility {
        trigger = Triggers.attached.dies()
        effect = Effects.DrawCards(1, EffectTarget.ControllerOfTriggeringEntity)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Dan Murayama Scott"
        flavorText = "The tale of her life was already written, but that didn't mean she could predict the ending."
        imageUri = "https://cards.scryfall.io/normal/front/9/0/90d3ef99-6c86-4d70-a9a4-9c3cd4eb9538.jpg?1783939798"
        ruling(
            "2013-09-15",
            "If the target of an Aura is illegal when it tries to resolve, the Aura won't resolve. The Aura doesn't enter the battlefield, so you won't get to draw a card."
        )
    }
}
