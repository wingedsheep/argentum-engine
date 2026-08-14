package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Tangled Colony
 * {1}{B}
 * Creature — Rat
 * 3/2
 *
 * This creature can't block.
 * When this creature dies, create X 1/1 black Rat creature tokens with "This token can't block,"
 * where X is the amount of damage dealt to it this turn.
 *
 * X is last-known information (CR 608.2h): the entity is already in the graveyard when the dies
 * trigger resolves, so the count reads the per-turn damage tally captured onto the
 * `ZoneChangeEvent` — [DynamicAmounts.lastKnownDamageDealtToSource]. Lethal damage isn't a cap:
 * damage in excess of toughness, and non-lethal damage dealt earlier in the turn that this
 * creature survived, both count.
 */
val TangledColony = card("Tangled Colony") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Rat"
    power = 3
    toughness = 2
    oracleText = "This creature can't block.\n" +
        "When this creature dies, create X 1/1 black Rat creature tokens with \"This token can't " +
        "block,\" where X is the amount of damage dealt to it this turn."

    staticAbility {
        ability = CantBlock(GroupFilter.source())
    }

    triggeredAbility {
        trigger = Triggers.Dies
        effect = Effects.CreateToken(
            count = DynamicAmounts.lastKnownDamageDealtToSource(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Rat"),
            staticAbilities = listOf(CantBlock(GroupFilter.source())),
            imageUri = "https://cards.scryfall.io/normal/front/1/e/1e0205f2-25c1-403b-b408-56e3f2d63b4d.jpg?1783915000",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "113"
        artist = "Filip Burburan"
        flavorText = "Clawing for scraps and driven mad by hunger, the individual rats became a " +
            "writhing mass."
        imageUri = "https://cards.scryfall.io/normal/front/7/7/77111ce8-6469-4bf7-882a-4ded1e5d7cad.jpg?1783915099"
    }
}
