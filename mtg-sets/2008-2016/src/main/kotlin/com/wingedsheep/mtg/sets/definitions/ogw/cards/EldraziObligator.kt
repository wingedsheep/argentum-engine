package com.wingedsheep.mtg.sets.definitions.ogw.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.ManaCost
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Eldrazi Obligator
 * {2}{R}
 * Creature — Eldrazi
 * 3/1
 * Devoid (This card has no color.)
 * When you cast this spell, you may pay {1}{C}. If you do, gain control of target creature until end
 * of turn, untap that creature, and it gains haste until end of turn.
 * Haste
 *
 * The cast trigger goes on the stack above the spell and the {1}{C} is paid as it resolves. Any
 * creature may be targeted, including an untapped one or one you already control (2016-01-22 ruling).
 */
val EldraziObligator = card("Eldrazi Obligator") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Eldrazi"
    power = 3
    toughness = 1
    oracleText = "Devoid (This card has no color.)\n" +
        "When you cast this spell, you may pay {1}{C}. If you do, gain control of target creature until end of turn, " +
        "untap that creature, and it gains haste until end of turn. ({C} represents colorless mana.)\n" +
        "Haste"

    keywords(Keyword.DEVOID, Keyword.HASTE)

    triggeredAbility {
        trigger = Triggers.self.isCast()
        val creature = target(TargetFilter.Creature)
        effect = Effects.MayPay(
            ManaCost.parse("{1}{C}"),
            Effects.GainControl(creature, Duration.EndOfTurn) then
                Effects.Untap(creature) then
                Effects.GrantKeyword(Keyword.HASTE, creature),
        )
        description = "When you cast this spell, you may pay {1}{C}. If you do, gain control of target creature " +
            "until end of turn, untap that creature, and it gains haste until end of turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "96"
        artist = "Jason Felix"
        imageUri = "https://cards.scryfall.io/normal/front/d/f/dfaa8b16-7c12-44e2-8ddb-17608d491c41.jpg?1783937909"
    }
}
