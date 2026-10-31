package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.CardNumericProperty

/**
 * Atemsis, All-Seeing — Core Set 2020 #46 (canonical printing)
 * {3}{U}{U}{U}
 * Legendary Creature — Sphinx
 * 4/5
 * Flying
 * {2}{U}, {T}: Draw two cards, then discard a card.
 * Whenever Atemsis deals damage to an opponent, you may reveal your hand. If cards with at least
 * six different mana values are revealed this way, that player loses the game.
 */
val AtemsisAllSeeing = card("Atemsis, All-Seeing") {
    manaCost = "{3}{U}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Sphinx"
    power = 4
    toughness = 5
    oracleText = "Flying\n{2}{U}, {T}: Draw two cards, then discard a card.\nWhenever Atemsis deals damage to an " +
        "opponent, you may reveal your hand. If cards with at least six different mana values are revealed " +
        "this way, that player loses the game."

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{U}"), Costs.Tap)
        effect = Patterns.Hand.loot(draw = 2, discard = 1)
    }

    // The condition sits inside the "may": only a revealed hand can win the game.
    triggeredAbility {
        trigger = Triggers.self.dealsDamage(Recipient.Opponent)
        effect = Effects.May(
            Effects.RevealHand(EffectTarget.Controller) then Effects.If(
                Conditions.CompareAmounts(
                    DynamicAmounts.zone(Player.You, Zone.HAND).distinctValues(CardNumericProperty.MANA_VALUE),
                    ComparisonOperator.GTE,
                    6,
                ),
                Effects.LoseGame(
                    target = EffectTarget.PlayerRef(Player.TriggeringPlayer),
                    message = "Atemsis, All-Seeing revealed six different mana values",
                ),
            ),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "46"
        artist = "Ryan Pancoast"
        imageUri = "https://cards.scryfall.io/normal/front/b/8/b8f527d1-25c0-49b9-83d5-1278ca72d009.jpg?1783933016"
        ruling("2019-07-12", "Every Magic card has exactly one mana value. A card with no mana cost has a mana value of 0, and 0 can be one of the six mana values that count toward Atemsis's ability.")
        ruling("2019-07-12", "If a card in your hand has {X} in its mana cost, the value of X is considered to be 0.")
        ruling("2019-07-12", "The mana value of a split card is determined by combining its two halves. For example, the mana value of Fire//Ice in your hand is 4.")
        ruling("2019-07-12", "If an effect causes you to play with your hand revealed, you can still reveal your hand as Atemsis's last ability resolves. The ability works as normal.")
    }
}
