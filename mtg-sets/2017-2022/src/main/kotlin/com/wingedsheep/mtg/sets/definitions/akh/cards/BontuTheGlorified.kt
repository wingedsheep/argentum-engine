package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.CantBlockUnless
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bontu the Glorified
 * {2}{B}
 * Legendary Creature — God
 * 4/6
 * Menace, indestructible
 * Bontu can't attack or block unless a creature died under your control this turn.
 * {1}{B}, Sacrifice another creature: Scry 1. Each opponent loses 1 life and you gain 1 life.
 *
 * The combat restriction is the [com.wingedsheep.mtg.sets.definitions.dft.cards.HazoretGodseeker]
 * pair of statics over [Conditions.ControlledCreatureDiedThisTurn], which reads the controller's
 * creatures-died-this-turn tally — so a creature sacrificed to Bontu's own ability unlocks it.
 */
val BontuTheGlorified = card("Bontu the Glorified") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — God"
    oracleText = "Menace, indestructible\n" +
        "Bontu can't attack or block unless a creature died under your control this turn.\n" +
        "{1}{B}, Sacrifice another creature: Scry 1. Each opponent loses 1 life and you gain 1 life."
    power = 4
    toughness = 6

    keywords(Keyword.MENACE, Keyword.INDESTRUCTIBLE)

    staticAbility {
        ability = CantAttackUnless(Conditions.ControlledCreatureDiedThisTurn)
    }
    staticAbility {
        ability = CantBlockUnless(Conditions.ControlledCreatureDiedThisTurn)
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}{B}"), Costs.SacrificeAnother(GameObjectFilter.Creature))
        effect = Effects.Scry(1) then
            Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then
            Effects.GainLife(1)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "82"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/4/b/4bc07386-909f-4946-9120-3acb58622e2f.jpg"
    }
}
