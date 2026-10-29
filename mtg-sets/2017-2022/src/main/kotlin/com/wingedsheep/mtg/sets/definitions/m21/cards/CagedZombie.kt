package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Caged Zombie
 * {2}{B}
 * Creature — Zombie
 * 2/3
 *
 * {1}{B}, {T}: Each opponent loses 2 life. Activate only if a creature died this turn.
 *
 * The activation gate is Undercity Scrounger's global `Conditions.CreatureDiedThisTurn`.
 */
val CagedZombie = card("Caged Zombie") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 3
    oracleText = "{1}{B}, {T}: Each opponent loses 2 life. Activate only if a creature died this turn."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.Tap)
        effect = Effects.LoseLife(2, EffectTarget.PlayerRef(Player.EachOpponent))
        restrictions = listOf(ActivationRestriction.OnlyIfCondition(Conditions.CreatureDiedThisTurn))
        description = "Each opponent loses 2 life."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "91"
        artist = "Josh Hass"
        flavorText = "\"Six consecutive life sentences\" takes on a new meaning when necromancy is involved."
        imageUri = "https://cards.scryfall.io/normal/front/f/8/f8067745-35b6-4abd-9ae9-712159a26c89.jpg?1783930711"
    }
}
