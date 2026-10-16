package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.IncrementAbilityResolutionCountEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Sizzling Soloist
 * {3}{R}
 * Creature — Human Citizen
 * Alliance — Whenever another creature you control enters, target creature an opponent controls
 * can't block this turn. If this is the second time this ability has resolved this turn, that
 * creature attacks during its controller's next combat phase if able.
 * 3/2
 */
val SizzlingSoloist = card("Sizzling Soloist") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Human Citizen"
    power = 3
    toughness = 2
    oracleText = "Alliance — Whenever another creature you control enters, target creature an opponent " +
        "controls can't block this turn. If this is the second time this ability has resolved this turn, " +
        "that creature attacks during its controller's next combat phase if able."

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.youControl()).enters()
        val victim = target(TargetFilter.CreatureOpponentControls)
        effect = Effects.CantBlock(victim) then
            IncrementAbilityResolutionCountEffect then
            Effects.If(
                condition = Conditions.SourceAbilityResolvedNTimes(2),
                then = Effects.MarkMustAttackNextCombat(victim)
            )
        description = "Alliance — Whenever another creature you control enters, target creature an opponent " +
            "controls can't block this turn. If this is the second time this ability has resolved this turn, " +
            "that creature attacks during its controller's next combat phase if able."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Véronique Meignaud"
        imageUri = "https://cards.scryfall.io/normal/front/9/5/958dfd59-7dbd-45e0-b6e1-e432c78b6878.jpg?1783923111"
    }
}
