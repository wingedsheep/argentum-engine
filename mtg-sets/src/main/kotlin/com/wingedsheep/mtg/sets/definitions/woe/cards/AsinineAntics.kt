package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Asinine Antics
 * {2}{U}{U}
 * Sorcery
 *
 * You may cast this spell as though it had flash if you pay {2} more to cast it.
 * For each creature your opponents control, create a Cursed Role token attached to that creature.
 *
 * The flash unlock is the Ghitu Fire shape — [KeywordAbility.flashKicker], an optional additional
 * cost that only changes timing, never the effect.
 *
 * The Roles are **not** targeted ("for each creature your opponents control"), so hexproof and shroud
 * don't stop them, and the spell still resolves if every opposing creature leaves before resolution.
 * The Cursed Role sets the enchanted creature's base P/T to 1/1 in Layer 7b, and the one-Role-per-
 * controller rule (CR 704.5s) that discards an older Role is a state-based action handled by the
 * engine, not by this script.
 */
val AsinineAntics = card("Asinine Antics") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "You may cast this spell as though it had flash if you pay {2} more to cast it.\n" +
        "For each creature your opponents control, create a Cursed Role token attached to that " +
        "creature. (If you control another Role on it, put that one into the graveyard. Enchanted " +
        "creature is 1/1.)"

    keywordAbility(KeywordAbility.flashKicker("{2}"))

    spell {
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesOpponentsControl,
            effect = Effects.CreateRoleToken("Cursed Role", EffectTarget.Self),
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "42"
        artist = "Brent Hollowell"
        imageUri = "https://cards.scryfall.io/normal/front/5/0/50b96a97-0d7d-4e05-9e2f-0b99a039b655.jpg?1783915124"

        ruling(
            "2023-09-01",
            "Roles are colorless enchantment tokens. Each one has the Aura and Role subtypes and " +
                "the enchant creature ability."
        )
        ruling(
            "2023-09-01",
            "If a permanent has more than one Role attached to it controlled by the same player, " +
                "each of those Roles except the one with the most recent timestamp is put into its " +
                "owner's graveyard. This is a state-based action."
        )
        ruling(
            "2023-09-01",
            "Hexproof and shroud won't prevent a Role from becoming attached to a permanent if the " +
                "ability creating that Role attached to that permanent doesn't target it."
        )
    }
}
