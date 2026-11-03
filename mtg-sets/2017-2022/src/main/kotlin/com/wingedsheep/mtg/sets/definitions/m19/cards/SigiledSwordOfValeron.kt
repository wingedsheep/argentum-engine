package com.wingedsheep.mtg.sets.definitions.m19.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantSubtype
import com.wingedsheep.sdk.scripting.ModifyStats
import com.wingedsheep.sdk.scripting.effects.EnteringAttackDefender

/**
 * Sigiled Sword of Valeron
 * {3}
 * Artifact — Equipment
 * Equipped creature gets +2/+0, has vigilance, and is a Knight in addition to its other types.
 * Whenever equipped creature attacks, create a 2/2 white Knight creature token with vigilance
 * that's attacking.
 * Equip {3}
 *
 * The token enters untapped and attacking a defender its controller chooses (CR 508.4) — not
 * necessarily the one the equipped creature attacks.
 */
val SigiledSwordOfValeron = card("Sigiled Sword of Valeron") {
    manaCost = "{3}"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature gets +2/+0, has vigilance, and is a Knight in addition to its other types.\n" +
        "Whenever equipped creature attacks, create a 2/2 white Knight creature token with vigilance that's attacking.\n" +
        "Equip {3}"

    staticAbility {
        ability = ModifyStats(2, 0, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantKeyword(Keyword.VIGILANCE)
    }

    staticAbility {
        ability = GrantSubtype(Subtype.KNIGHT.value, Filters.EquippedCreature)
    }

    triggeredAbility {
        trigger = Triggers.attached.attacks()
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Knight"),
            keywords = setOf(Keyword.VIGILANCE),
            attacking = true,
            attackDefender = EnteringAttackDefender.CONTROLLER_CHOOSES,
            imageUri = "https://cards.scryfall.io/normal/front/f/d/fd8ca5ee-7b93-4248-8f4a-b88261ee1eed.jpg?1783934470",
        )
    }

    equipAbility("{3}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "244"
        artist = "John Stanko"
        imageUri = "https://cards.scryfall.io/normal/front/4/9/497d08a8-cffe-4164-a941-d8c8c85644c7.jpg?1783934508"
        ruling("2018-07-13", "You choose which player or planeswalker the token is attacking. It doesn't have to be attacking the same player or planeswalker that the equipped creature is attacking.")
        ruling("2018-07-13", "Although the token is attacking, it was never declared as an attacking creature (for the purposes of abilities that trigger whenever a creature attacks, for example).")
    }
}
