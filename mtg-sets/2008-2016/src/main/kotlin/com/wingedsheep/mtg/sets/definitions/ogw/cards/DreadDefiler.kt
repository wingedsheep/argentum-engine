package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Dread Defiler
 * {6}{B}
 * Creature — Eldrazi
 * 6/8
 * Devoid (This card has no color.)
 * {3}{C}, Exile a creature card from your graveyard: Target opponent loses life equal to the
 * exiled card's power. ({C} represents colorless mana.)
 *
 * The exile is part of the activation cost, so the card is already in exile when the ability
 * resolves; [EffectTarget.ExiledAsCost] names the card this activation exiled, and its power is
 * the same in exile as it was in the graveyard (ruling: use its power as it last existed there).
 */
val DreadDefiler = card("Dread Defiler") {
    manaCost = "{6}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Eldrazi"
    power = 6
    toughness = 8
    oracleText = "Devoid (This card has no color.)\n" +
        "{3}{C}, Exile a creature card from your graveyard: Target opponent loses life equal to " +
        "the exiled card's power. ({C} represents colorless mana.)"

    keywords(Keyword.DEVOID)

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{3}{C}"),
            Costs.ExileFromGraveyard(1, GameObjectFilter.Creature)
        )
        val opponent = target(Targets.Opponent)
        effect = Effects.LoseLife(DynamicAmounts.powerOf(EffectTarget.ExiledAsCost()), opponent)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "68"
        artist = "Slawomir Maniak"
        flavorText = "It left behind an echoing emptiness more horrible than the grave."
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2b26769-4705-4b09-94b4-1063e67041d1.jpg?1783937915"
        ruling("2016-01-22", "A creature token is not a creature card. Although a token does go to the graveyard briefly if it dies, it ceases to exist before you have the opportunity to do anything.")
        ruling("2016-01-22", "Use the card’s power as it last existed in your graveyard to determine how much life is lost.")
    }
}
