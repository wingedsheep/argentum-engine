package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Swarm Intelligence — Hour of Devastation #50
 * {6}{U} Enchantment
 *
 * "Whenever you cast an instant or sorcery spell, you may copy that spell. You may choose new
 *  targets for the copy."
 *
 * An optional [Effects.CopyTargetSpell] of the triggering spell (Wandering Archaic's unpaid branch
 * without the tax). The copy still resolves if the original was countered first — the executor
 * copies the spell as it last existed on the stack.
 */
val SwarmIntelligence = card("Swarm Intelligence") {
    manaCost = "{6}{U}"
    typeLine = "Enchantment"
    oracleText = "Whenever you cast an instant or sorcery spell, you may copy that spell. " +
        "You may choose new targets for the copy."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.May(Effects.CopyTargetSpell(target = EffectTarget.TriggeringEntity))
        description = "Whenever you cast an instant or sorcery spell, you may copy that spell. " +
            "You may choose new targets for the copy."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "50"
        artist = "Daniel Ljunggren"
        flavorText = "The Locust God's children were numberless, relentlessly eating away every trace of the city's protection."
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2bec26d-20ba-4f31-a153-2c93a4f474e5.jpg?1783936046"
        ruling("2017-07-14", "Swarm Intelligence can copy the spell even if it's countered before Swarm Intelligence's triggered ability resolves.")
        ruling("2017-07-14", "The copy is created on the stack, so it's not \"cast.\" Abilities that trigger when a player casts a spell (such as Swarm Intelligence's own ability) won't trigger.")
    }
}
