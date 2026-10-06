package com.wingedsheep.mtg.sets.definitions.lea.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cockatrice
 * {3}{G}{G}
 * Creature — Cockatrice
 * 2/4
 * Flying
 * Whenever this creature blocks or becomes blocked by a non-Wall creature, destroy that creature
 * at end of combat.
 *
 * The Venom shape on the creature itself: the combat trigger binds each matching combat partner as
 * the triggering entity (one trigger per partner — `oncePerCombat` defaults off when a partner
 * filter is given) and schedules a delayed end-of-combat destroy aimed at it.
 */
val Cockatrice = card("Cockatrice") {
    manaCost = "{3}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Cockatrice"
    power = 2
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever this creature blocks or becomes blocked by a non-Wall creature, destroy that " +
        "creature at end of combat."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.blocksOrBecomesBlocked(GameObjectFilter.Creature.notSubtype(Subtype.WALL))
        effect = Effects.CreateDelayedTrigger(
            step = Step.END_COMBAT,
            effect = Effects.Destroy(EffectTarget.TriggeringEntity),
        )
        description = "Whenever this creature blocks or becomes blocked by a non-Wall creature, " +
            "destroy that creature at end of combat."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "189"
        artist = "Dan Frazier"
        imageUri = "https://cards.scryfall.io/normal/front/9/c/9cd91814-6177-4a3d-a1c1-a3be7d7c7957.jpg?1783948678"
    }
}
