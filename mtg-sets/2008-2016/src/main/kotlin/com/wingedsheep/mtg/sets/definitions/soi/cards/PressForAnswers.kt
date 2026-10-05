package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Press for Answers — Shadows over Innistrad #80
 * {1}{U} · Sorcery
 *
 * Tap target creature. It doesn't untap during its controller's next untap step.
 * Investigate.
 *
 * The Clue rides on the whole spell resolving: if the only target is illegal on resolution the
 * spell doesn't resolve and you don't investigate (2016-04-08 ruling).
 */
val PressForAnswers = card("Press for Answers") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Tap target creature. It doesn't untap during its controller's next untap step.\n" +
        "Investigate. (Create a Clue token. It's an artifact with \"{2}, Sacrifice this token: Draw a card.\")"

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Tap(creature) then
            Effects.GrantKeyword(
                AbilityFlag.DOESNT_UNTAP,
                creature,
                Duration.UntilAfterAffectedControllersNextUntap
            ) then
            Effects.Investigate()
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "80"
        artist = "Steve Prescott"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5ec8b8bf-7b02-4f5e-bbd3-9560f9888192.jpg?1783937790"
        ruling(
            "2016-04-08",
            "You can't cast a spell without choosing legal targets. If all of those targets become " +
                "illegal, the spell doesn't resolve and you won't investigate.",
        )
    }
}
