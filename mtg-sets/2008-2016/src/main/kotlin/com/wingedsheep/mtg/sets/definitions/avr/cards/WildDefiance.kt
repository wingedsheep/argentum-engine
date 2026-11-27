package com.wingedsheep.mtg.sets.definitions.avr.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Wild Defiance
 * {2}{G}
 * Enchantment
 * Whenever a creature you control becomes the target of an instant or sorcery spell, that
 * creature gets +3/+3 until end of turn.
 *
 * The targeting spell may be controlled by any player, and only instant and sorcery spells count —
 * abilities and other spells don't.
 */
val WildDefiance = card("Wild Defiance") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Whenever a creature you control becomes the target of an instant or sorcery spell, " +
        "that creature gets +3/+3 until end of turn."

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.youControl())
            .becomesTarget(of = GameObjectFilter.InstantOrSorcery, spellsOnly = true)
        effect = Effects.ModifyStats(3, 3, EffectTarget.TriggeringEntity)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "203"
        artist = "Slawomir Maniak"
        flavorText = "\"When civilization reaches out its greedy hand, take it off at the wrist.\"\n—Garruk Wildspeaker"
        imageUri = "https://cards.scryfall.io/normal/front/7/e/7eaa6be7-2c6f-4442-85d1-ae31ad87fd98.jpg?1783940657"
    }
}
