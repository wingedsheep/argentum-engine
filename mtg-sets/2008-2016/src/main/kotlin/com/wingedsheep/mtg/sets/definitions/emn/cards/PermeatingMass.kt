package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Permeating Mass
 * {G}
 * Creature — Spirit
 * 1/3
 * Whenever this creature deals combat damage to a creature, that creature becomes a copy of this
 * creature.
 *
 * The copy reads this creature's copiable values as it last existed on the battlefield (ruling
 * 2016-07-13: if Permeating Mass is dealt lethal damage in the same combat, the damaged creature
 * still becomes a copy of it), so the trigger carries its source's last-known snapshot onto the stack.
 */
val PermeatingMass = card("Permeating Mass") {
    manaCost = "{G}"
    typeLine = "Creature — Spirit"
    power = 1
    toughness = 3
    oracleText = "Whenever this creature deals combat damage to a creature, that creature becomes a copy of this creature."

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyCreature)
        effect = Effects.EachPermanentBecomesCopyOfTarget(
            target = EffectTarget.Self,
            affected = EffectTarget.TriggeringEntity,
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "165"
        artist = "Joseph Meehan"
        flavorText = "Woodland geists concluded that Emrakul would be unable to warp the denizens of the forest if the geists got there first."
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b71290eb-b529-4fc3-ab94-fee0ab9e2169.jpg?1783937441"
    }
}
