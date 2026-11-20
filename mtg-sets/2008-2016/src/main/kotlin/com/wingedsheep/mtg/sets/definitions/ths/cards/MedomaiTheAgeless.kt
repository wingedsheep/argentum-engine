package com.wingedsheep.mtg.sets.definitions.ths.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttack
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Medomai the Ageless
 * {4}{W}{U}
 * Legendary Creature — Sphinx
 * 4/4
 * Flying
 * Whenever Medomai deals combat damage to a player, take an extra turn after this one.
 * Medomai can't attack during extra turns.
 *
 * The attack lock is a [CantAttack] on itself gated by [Conditions.IsExtraTurn], which reads
 * whether the current turn began as an extra turn — whoever's turn it is, per the 2013-09-15
 * ruling (an opponent who steals Medomai during their own extra turn can't attack with it either).
 */
val MedomaiTheAgeless = card("Medomai the Ageless") {
    manaCost = "{4}{W}{U}"
    colorIdentity = "WU"
    typeLine = "Legendary Creature — Sphinx"
    power = 4
    toughness = 4
    oracleText = "Flying\n" +
        "Whenever Medomai deals combat damage to a player, take an extra turn after this one.\n" +
        "Medomai can't attack during extra turns."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.TakeExtraTurn()
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = CantAttack(GroupFilter.source()),
            condition = Conditions.IsExtraTurn,
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "196"
        artist = "David Palumbo"
        imageUri = "https://cards.scryfall.io/normal/front/b/5/b5f4b3cf-2c78-42c9-9873-3758c7f7bb7d.jpg?1783939729"
        ruling("2013-09-15", "An \"extra turn\" is any turn created by a spell or ability. Notably, it doesn't include additional turns taken in tournaments after time expires for a round.")
        ruling("2013-09-15", "It doesn't matter whose turn it is. In very rare cases, an opponent may be taking an extra turn and gain control of Medomai. Medomai can't attack during that turn.")
    }
}
