package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Goblin Bird-Grabber (M20 #142)
 * {1}{R}  Creature — Goblin  2/1
 *
 * {R}: This creature gains flying until end of turn. Activate only if you control a creature
 * with flying.
 */
val GoblinBirdGrabber = card("Goblin Bird-Grabber") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin"
    oracleText = "{R}: This creature gains flying until end of turn. " +
        "Activate only if you control a creature with flying."
    power = 2
    toughness = 1

    activatedAbility {
        cost = Costs.Mana("{R}")
        effect = Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self)
        restrictions = listOf(
            ActivationRestriction.OnlyIfCondition(
                Conditions.YouControl(GameObjectFilter.Creature.withKeyword(Keyword.FLYING))
            )
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "142"
        artist = "Anna Podedworna"
        flavorText = "\"I opens my mouth to catches the bugs!\""
        imageUri = "https://cards.scryfall.io/normal/front/5/4/54fbbf39-cd52-474a-908e-b7a78cb5eb5b.jpg"
    }
}
