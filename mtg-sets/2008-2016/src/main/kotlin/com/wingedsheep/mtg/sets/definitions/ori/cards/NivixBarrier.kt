package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Nivix Barrier
 * {3}{U}
 * Creature — Illusion Wall
 * 0/4
 * Flash
 * Defender
 * When this creature enters, target attacking creature gets -4/-0 until end of turn.
 */
val NivixBarrier = card("Nivix Barrier") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Illusion Wall"
    power = 0
    toughness = 4
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\nDefender (This creature can't attack.)\nWhen this creature enters, target attacking creature gets -4/-0 until end of turn."

    keywords(Keyword.FLASH, Keyword.DEFENDER)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target attacking creature", Targets.AttackingCreature)
        effect = Effects.ModifyStats(-4, 0, t)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "66"
        artist = "Mathias Kollros"
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0b9b968d-b0cc-411d-9366-8358be28aef2.jpg?1783938350"
    }
}
