package com.wingedsheep.mtg.sets.definitions.p02.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sylvan Basilisk
 * {3}{G}{G}
 * Creature — Basilisk
 * 2/4
 *
 * `Triggers.self.becomesBlocked(by = Creature)` fires once per blocker with that blocker as the
 * triggering entity, so a double block destroys both blockers — one trigger each (Ogre Leadfoot's shape).
 */
val SylvanBasilisk = card("Sylvan Basilisk") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Basilisk"
    power = 2
    toughness = 4
    oracleText = "Whenever this creature becomes blocked by a creature, destroy that creature."

    triggeredAbility {
        trigger = Triggers.self.becomesBlocked(by = GameObjectFilter.Creature)
        effect = Effects.Destroy(EffectTarget.TriggeringEntity)
        description = "Whenever this creature becomes blocked by a creature, destroy that creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "146"
        artist = "Ron Spencer"
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9c09886-a733-4797-a489-46150cecfc13.jpg?1783946448"
    }
}
