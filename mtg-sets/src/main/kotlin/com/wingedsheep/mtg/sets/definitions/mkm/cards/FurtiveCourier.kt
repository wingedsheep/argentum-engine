package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Furtive Courier {2}{U}
 * Creature — Merfolk Advisor
 * 3/2
 *
 * This creature can't be blocked as long as you've sacrificed an artifact this turn.
 * Whenever this creature attacks, draw a card, then discard a card.
 *
 * The evasion is a [ConditionalStaticAbility], not an attack trigger: "as long as" is a
 * continuous ability re-evaluated every time the projection is rebuilt, which is what makes the
 * card's ruling fall out for free — sacrificing an artifact after blockers are declared doesn't
 * unblock it, because CR 509 has already locked the block in. Conversely, sacrificing an artifact
 * before blockers are declared makes it unblockable even though it was already attacking.
 *
 * [Conditions.SacrificedArtifactThisTurn] is controller-scoped, so the *defending* player cracking
 * a Clue never turns this on.
 */
val FurtiveCourier = card("Furtive Courier") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Merfolk Advisor"
    power = 3
    toughness = 2
    oracleText = "This creature can't be blocked as long as you've sacrificed an artifact this turn.\n" +
        "Whenever this creature attacks, draw a card, then discard a card."

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = GrantKeyword(AbilityFlag.CANT_BE_BLOCKED.name, GroupFilter.source()),
            condition = Conditions.SacrificedArtifactThisTurn
        )
    }

    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Patterns.Hand.loot()
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "59"
        artist = "Mark Behm"
        flavorText = "\"Glad to see you made it in one piece. Next time, try not looking so " +
            "obviously nervous.\"\n—Linghu of the Foundway Associates"
        imageUri = "https://cards.scryfall.io/normal/front/6/f/6f359fc2-b9e4-4a01-9d04-442bb160b01e.jpg?1783912908"
        ruling(
            "2024-02-02",
            "Sacrificing an artifact after Furtive Courier has been blocked won't cause it to " +
                "become unblocked."
        )
    }
}
