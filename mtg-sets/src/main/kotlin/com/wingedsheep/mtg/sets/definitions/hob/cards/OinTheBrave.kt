package com.wingedsheep.mtg.sets.definitions.hob.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.storied
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Óin the Brave
 * {1}{R}
 * Legendary Creature — Dwarf Warrior
 * 1/3
 *
 * Storied.
 * As long as you have an enduring story, Óin gets +1/+0 and has haste.
 * {1}, {T}, Discard a card: Draw a card.
 *
 * The haste half is the reason storied is a continuously-checked state-based action rather than an
 * enters-the-battlefield trigger: Óin is a two-drop, so on the turn he lands you will rarely control
 * three artifacts/legendaries/Sagas. A trigger would sample the count once and Óin would never gain
 * haste on any later turn; the CR 702.195a check re-reads it every SBA poll, so he has haste from the
 * turn your third qualifying permanent arrives onward — including a turn where that third permanent
 * is a hasty attacker cast before him.
 *
 * The looter ability is ungated — it works with or without an enduring story — so it sits outside the
 * conditional statics as an ordinary activated ability.
 */
val OinTheBrave = card("Óin the Brave") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — Dwarf Warrior"
    oracleText = "Storied (If you control three or more artifacts, legendaries, and/or Sagas, you " +
        "have an enduring story for the rest of the game.)\n" +
        "As long as you have an enduring story, Óin gets +1/+0 and has haste.\n" +
        "{1}, {T}, Discard a card: Draw a card."
    power = 1
    toughness = 3

    storied()

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = ModifyStats(powerBonus = 1, toughnessBonus = 0, filter = GroupFilter.source()),
            condition = Conditions.YouHaveEnduringStory
        )
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(Keyword.HASTE, GroupFilter.source()),
            condition = Conditions.YouHaveEnduringStory
        )
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.DiscardCard)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "106"
        artist = "Colin Boyer"
        flavorText = "When the heart of a Dwarf is wakened by gold, he grows suddenly bold and fierce."
        imageUri = "https://cards.scryfall.io/normal/front/9/9/9984b9ef-e81c-48f4-aa33-0504171a2d3c.jpg?1785496200"
    }
}
