package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/** "its controller loses 2 life" — the granted ability is controlled by the enchanted creature's
 *  controller, so [EffectTarget.Controller] is exactly "its controller". */
private val controllerLosesTwo = Effects.LoseLife(2, EffectTarget.Controller)

/**
 * Sinister Possession — Dragon's Maze #29
 * {B} · Enchantment — Aura
 *
 * Enchant creature
 * Whenever enchanted creature attacks or blocks, its controller loses 2 life.
 *
 * The Contaminated Bond idiom: two [GrantTriggeredAbility] statics over [Filters.EnchantedCreature]
 * (attacks, blocks). The triggers live on the creature, so the life loss lands on the creature's
 * controller rather than the Aura's, and the blocks half fires (an ATTACHED-bound blocks trigger
 * would not).
 */
val SinisterPossession = card("Sinister Possession") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Aura"
    oracleText = "Enchant creature\n" +
        "Whenever enchanted creature attacks or blocks, its controller loses 2 life."

    auraTarget = TargetObject(filter = TargetFilter.Creature)

    // "Whenever enchanted creature attacks ..."
    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.self.attacks(),
                effect = controllerLosesTwo,
                descriptionOverride = "Whenever this creature attacks, its controller loses 2 life.",
            ),
            filter = Filters.EnchantedCreature,
        )
    }

    // "... or blocks, its controller loses 2 life."
    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.self.blocks(),
                effect = controllerLosesTwo,
                descriptionOverride = "Whenever this creature blocks, its controller loses 2 life.",
            ),
            filter = Filters.EnchantedCreature,
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "29"
        artist = "Anthony Palumbo"
        flavorText = "\"You caught something on a cold night's stroll? No, I'd say something caught you.\"\n" +
            "—Mezim Magrah, civic healer"
        imageUri = "https://cards.scryfall.io/normal/front/7/f/7f54c15b-fec0-49a6-8a49-d1af4eeee40e.jpg?1783940039"
    }
}
