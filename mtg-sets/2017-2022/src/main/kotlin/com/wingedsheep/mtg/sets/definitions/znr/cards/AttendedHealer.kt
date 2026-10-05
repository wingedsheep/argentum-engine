package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Attended Healer — Zendikar Rising #6 (canonical printing; reprinted in Jumpstart 2022)
 * {3}{W} · Creature — Kor Cleric · 2/3
 *
 * Whenever you gain life for the first time each turn, create a 1/1 white Cat creature token.
 * {2}{W}: Another target Cleric gains lifelink until end of turn.
 *
 * "Another" excludes the Healer itself (it is a Cleric), so the activated ability targets any
 * other Cleric permanent on the battlefield — either player's.
 */
val AttendedHealer = card("Attended Healer") {
    manaCost = "{3}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Kor Cleric"
    oracleText = "Whenever you gain life for the first time each turn, create a 1/1 white Cat creature token.\n" +
        "{2}{W}: Another target Cleric gains lifelink until end of turn."
    power = 2
    toughness = 3

    triggeredAbility {
        trigger = Triggers.you.gainsLife(true)
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Cat"),
            imageUri = "https://cards.scryfall.io/normal/front/5/f/5f458f39-27b6-4121-bda9-1a0d1b42f5fb.jpg?1783929500",
        )
    }

    activatedAbility {
        cost = Costs.Mana("{2}{W}")
        val t = target(TargetFilter.Permanent.withSubtype("Cleric").other())
        effect = Effects.GrantKeyword(Keyword.LIFELINK, t)
        description = "{2}{W}: Another target Cleric gains lifelink until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "6"
        artist = "Wisnu Tan"
        flavorText = "\"My cats have taught me more about the sacred than anything in some old ruin.\""
        imageUri = "https://cards.scryfall.io/normal/front/6/c/6ca80b5a-3943-41ca-8cd1-8e3db1b2a1c8.jpg?1783929423"

        ruling(
            "2020-09-25",
            "An ability that triggers whenever you gain life \"for the first time each turn\" won't trigger " +
                "if you gain life during a turn before the permanent with that ability is on the battlefield, " +
                "even if you gain life again later in the turn.",
        )
        ruling(
            "2020-09-25",
            "In a Two-Headed Giant game, life gained by your teammate won't cause the ability to trigger, " +
                "even though it caused your team's life total to increase.",
        )
    }
}
