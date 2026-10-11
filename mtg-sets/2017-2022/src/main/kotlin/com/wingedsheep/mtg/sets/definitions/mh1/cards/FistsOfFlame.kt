package com.wingedsheep.mtg.sets.definitions.mh1.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Fists of Flame
 * {1}{R}
 * Instant
 * Draw a card. Until end of turn, target creature gains trample and gets +1/+0 for each card
 * you've drawn this turn.
 *
 * The draw runs first, so the bonus counts the card Fists of Flame itself drew. The +X/+0 is
 * `ModifyStats` over `DynamicAmounts.cardsDrawnThisTurn(You)` (`TurnTracking(You, CARDS_DRAWN)`),
 * which the executor evaluates once at resolution — the bonus is locked in and later draws don't
 * grow it (Scryfall ruling 2019-06-14).
 */
val FistsOfFlame = card("Fists of Flame") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Draw a card. Until end of turn, target creature gains trample and gets +1/+0 " +
        "for each card you've drawn this turn. (A creature with trample can deal excess combat " +
        "damage to the player or planeswalker it's attacking.)"

    spell {
        val t = target(TargetFilter.Creature)
        effect = Effects.DrawCards(1) then
            Effects.GrantKeyword(Keyword.TRAMPLE, t) then
            Effects.ModifyStats(DynamicAmounts.cardsDrawnThisTurn(Player.You), DynamicAmounts.fixed(0), t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "123"
        artist = "Steve Argyle"
        flavorText = "\"Fire has always been my most faithful companion.\"\n—Jared Carthalion"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4fa3c0d8-0a3e-43b0-b81e-a3d22ce6a4b2.jpg?1783933113"
        ruling("2019-06-14", "If the target creature is an illegal target by the time Fists of Flame tries to resolve, the spell doesn't resolve. You won't draw a card.")
        ruling("2019-06-14", "If a spell or ability causes you to put cards into your hand without specifically using the word \"draw,\" Fists of Flame's effect doesn't count them.")
        ruling("2019-06-14", "The bonus that the target creature receives is locked in as Fists of Flame resolves, counting the card drawn during its first effect. The creature won't get an additional +1/+0 if you draw another card later in the turn.")
    }
}
