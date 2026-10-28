package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.GrantTriggeredAbility
import com.wingedsheep.sdk.scripting.TriggeredAbility

/**
 * Infantry Shield — Tarkir: Dragonstorm Commander #35
 * {2}{R} · Artifact — Equipment
 *
 * Equipped creature has menace and mobilize X, where X is its power. (Whenever it attacks, create
 * X tapped and attacking 1/1 red Warrior creature tokens. Sacrifice them at the beginning of the
 * next end step.)
 * Equip {2}
 *
 * Mobilize is a triggered ability, so the equipped creature is granted the mobilize
 * trigger itself via [GrantTriggeredAbility] — SELF-bound, so it fires when that creature attacks
 * and its controller creates the Warriors. "Its power" is the equipped creature's power as the
 * trigger resolves ([DynamicAmounts.sourcePower]: the granted ability's source is the creature),
 * exactly the token count the `mobilize(amount, …)` builder wires for a printed Mobilize X.
 */
val InfantryShield = card("Infantry Shield") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Artifact — Equipment"
    oracleText = "Equipped creature has menace and mobilize X, where X is its power. (Whenever it " +
        "attacks, create X tapped and attacking 1/1 red Warrior creature tokens. Sacrifice them at " +
        "the beginning of the next end step.)\n" +
        "Equip {2}"

    staticAbility {
        ability = GrantKeyword(Keyword.MENACE, Filters.EquippedCreature)
    }

    staticAbility {
        ability = GrantTriggeredAbility(
            ability = TriggeredAbility.create(
                trigger = Triggers.self.attacks(),
                effect = Effects.CreateToken(
                    count = DynamicAmounts.sourcePower(),
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.RED),
                    creatureTypes = setOf("Warrior"),
                    imageUri = "https://cards.scryfall.io/normal/front/7/e/7edc0515-a130-45a7-aa09-0e23bba41587.jpg?1742506712",
                    tapped = true,
                    attacking = true,
                    sacrificeAtStep = Step.END
                ),
                descriptionOverride = "Mobilize X, where X is this creature's power (Whenever this " +
                    "creature attacks, create X tapped and attacking 1/1 red Warrior creature tokens. " +
                    "Sacrifice them at the beginning of the next end step.)"
            )
        )
    }

    equipAbility("{2}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "35"
        artist = "Xabi Gaztelua"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37332798-462e-49d3-b197-72411ccf67e5.jpg?1783907173"
        ruling("2025-04-04", "You choose the player, planeswalker, or battle each Warrior token is attacking. They don't all have to attack the same one, and they don't have to attack the same player, planeswalker, or battle as the creature with mobilize.")
        ruling("2025-04-04", "Although the Warrior tokens enter as attacking creatures, they were never declared as attacking creatures. Abilities that trigger whenever a creature attacks won't trigger when the tokens enter attacking.")
    }
}
