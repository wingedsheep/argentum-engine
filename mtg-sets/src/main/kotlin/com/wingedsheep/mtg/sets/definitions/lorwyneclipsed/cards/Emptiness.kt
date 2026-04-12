package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Emptiness
 * {4}{W/B}{W/B}
 * Creature — Elemental Incarnation
 * 3/5
 *
 * When this creature enters, if {W}{W} was spent to cast it, return target creature card
 * with mana value 3 or less from your graveyard to the battlefield.
 * When this creature enters, if {B}{B} was spent to cast it, put three -1/-1 counters
 * on up to one target creature.
 * Evoke {W/B}{W/B}
 */
val Emptiness = card("Emptiness") {
    manaCost = "{4}{W/B}{W/B}"
    typeLine = "Creature — Elemental Incarnation"
    power = 3
    toughness = 5
    oracleText = "When this creature enters, if {W}{W} was spent to cast it, return target creature card with mana value 3 or less from your graveyard to the battlefield.\nWhen this creature enters, if {B}{B} was spent to cast it, put three -1/-1 counters on up to one target creature.\nEvoke {W/B}{W/B}"

    evoke = "{W/B}{W/B}"

    // Black gate first (goes on stack first, resolves second)
    // so -1/-1 counters resolve after the reanimated creature enters
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.ManaSpentToCastIncludes(requiredBlack = 2)
        val creature = target("creature", TargetCreature(count = 1, optional = true))
        effect = Effects.AddCounters(Counters.MINUS_ONE_MINUS_ONE, 3, creature)
    }

    // White gate second (goes on stack second, resolves first)
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.ManaSpentToCastIncludes(requiredWhite = 2)
        val graveyardCreature = target("graveyard creature", TargetObject(
            filter = TargetFilter.CreatureInYourGraveyard.manaValueAtMost(3)
        ))
        effect = Effects.PutOntoBattlefield(graveyardCreature)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "222"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c6409eca-bef6-4f3a-8bbb-d69ec5dbfc13.jpg?1759144841"
    }
}
