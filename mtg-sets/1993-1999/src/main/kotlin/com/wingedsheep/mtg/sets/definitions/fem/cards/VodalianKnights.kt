package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vodalian Knights
 * {1}{U}{U}
 * Creature — Merfolk Knight
 * 2/2
 * First strike
 * This creature can't attack unless defending player controls an Island.
 * When you control no Islands, sacrifice this creature.
 * {U}: This creature gains flying until end of turn.
 *
 * "When you control no Islands" is a state trigger (CR 603.8) — it fires the moment the condition
 * becomes true and doesn't fire again until it has been false in between. The attack restriction
 * is bound to the *defending* player, which is what `CantAttackUnless` evaluates against.
 */
val VodalianKnights = card("Vodalian Knights") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Knight"
    oracleText = "First strike\n" +
        "This creature can't attack unless defending player controls an Island.\n" +
        "When you control no Islands, sacrifice this creature.\n" +
        "{U}: This creature gains flying until end of turn."
    power = 2
    toughness = 2

    keywords(Keyword.FIRST_STRIKE)

    staticAbility {
        ability = CantAttackUnless(Conditions.DefendingPlayerControlsLandType("Island"))
    }

    stateTriggeredAbility {
        condition = Conditions.YouControl(
            GameObjectFilter.Land.withSubtype(Subtype.ISLAND),
            negate = true,
        )
        effect = Effects.SacrificeTarget(EffectTarget.Self)
        description = "When you control no Islands, sacrifice this creature."
    }

    activatedAbility {
        cost = Costs.Mana("{U}")
        effect = Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "29"
        artist = "Susan Van Camp"
        flavorText = "Fear the Knight leaping from the water into the air, weapon ready."
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68d97e1b-2526-4740-b354-f158734d1f72.jpg?1783947907"
    }
}
