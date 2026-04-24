package com.wingedsheep.mtg.sets.definitions.lorwyneclipsed.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.EffectPatterns
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.targets.AnyTarget

/**
 * Vibrance
 * {3}{R/G}{R/G}
 * Creature — Elemental Incarnation
 * 4/4
 *
 * When this creature enters, if {R}{R} was spent to cast it, this creature deals 3 damage to any target.
 * When this creature enters, if {G}{G} was spent to cast it, search your library for a land card, reveal it,
 * put it into your hand, then shuffle. You gain 2 life.
 * Evoke {R/G}{R/G}
 */
val Vibrance = card("Vibrance") {
    manaCost = "{3}{R/G}{R/G}"
    typeLine = "Creature — Elemental Incarnation"
    power = 4
    toughness = 4
    oracleText = "When this creature enters, if {R}{R} was spent to cast it, this creature deals 3 damage to any target.\n" +
        "When this creature enters, if {G}{G} was spent to cast it, search your library for a land card, reveal it, put it into your hand, then shuffle. You gain 2 life.\n" +
        "Evoke {R/G}{R/G}"

    evoke = "{R/G}{R/G}"

    // Red gate: deals 3 damage to any target
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.ManaSpentToCastIncludes(requiredRed = 2)
        val damageTarget = target("any target", AnyTarget())
        effect = Effects.DealDamage(3, damageTarget)
    }

    // Green gate: search library for a land, reveal, to hand, shuffle, gain 2 life
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        triggerCondition = Conditions.ManaSpentToCastIncludes(requiredGreen = 2)
        effect = EffectPatterns.searchLibrary(
            filter = GameObjectFilter.Land,
            count = 1,
            destination = SearchDestination.HAND,
            reveal = true,
            shuffleAfter = true
        ).then(Effects.GainLife(2))
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "249"
        artist = "Jakub Kasper"
        imageUri = "https://cards.scryfall.io/normal/front/b/9/b9f71c3b-0840-475f-9c17-fdacbc7f3213.jpg?1767658560"
    }
}
