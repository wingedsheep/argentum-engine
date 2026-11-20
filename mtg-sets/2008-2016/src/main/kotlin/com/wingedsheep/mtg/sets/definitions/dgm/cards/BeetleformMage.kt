package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ActivationRestriction
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Beetleform Mage
 * {1}{G}{U}
 * Creature — Human Insect Wizard
 * 2/2
 * {G}{U}: This creature gets +2/+2 and gains flying until end of turn. Activate only once each turn.
 *
 * Canonical printing: Dragon's Maze, the card's earliest real printing.
 */
val BeetleformMage = card("Beetleform Mage") {
    manaCost = "{1}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Creature — Human Insect Wizard"
    power = 2
    toughness = 2
    oracleText = "{G}{U}: This creature gets +2/+2 and gains flying until end of turn. Activate only once each turn."

    activatedAbility {
        cost = Costs.Mana("{G}{U}")
        restrictions = listOf(ActivationRestriction.OncePerTurn)
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self) then
            Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "54"
        artist = "Marco Nelor"
        flavorText = "\"When the Simic say to take command of your life, we don't mean just your career.\"\n—Vorel of the Hull Clade"
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1e2f7d7f-4097-419b-8de0-b7bf28fc3a4b.jpg"
    }
}
