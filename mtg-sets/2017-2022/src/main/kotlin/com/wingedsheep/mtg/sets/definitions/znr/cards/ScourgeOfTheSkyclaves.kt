package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.minus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Scourge of the Skyclaves {1}{B}
 * Creature — Demon
 * * / *
 *
 * Kicker {4}{B}
 * When you cast this spell, if it was kicked, each player loses half their life, rounded up.
 * Scourge of the Skyclaves's power and toughness are each equal to 20 minus the highest life
 * total among players.
 *
 * - The cast trigger carries [Conditions.WasKicked] as an intervening "if"; it resolves before
 *   the creature spell (and even if the spell is countered). [Effects.ForEachPlayer] rebinds
 *   the controller per player, so [Effects.LoseHalfLife] reads each player's *own* life total.
 * - The P/T is a characteristic-defining ability ([dynamicStats]) that applies in every zone:
 *   `20 - GreatestAmongPlayers(LifeTotal)`. `Subtract` isn't floored at zero, so with a life
 *   total above 20 the Scourge has negative power and toughness (ruling 2020-09-25).
 */
val ScourgeOfTheSkyclaves = card("Scourge of the Skyclaves") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    oracleText = "Kicker {4}{B}\n" +
        "When you cast this spell, if it was kicked, each player loses half their life, rounded up.\n" +
        "Scourge of the Skyclaves's power and toughness are each equal to 20 minus the highest " +
        "life total among players."

    dynamicStats(
        DynamicAmounts.fixed(20) -
            DynamicAmounts.greatestAmongPlayers(DynamicAmounts.lifeTotal(Player.You), Player.Each)
    )

    keywordAbility(KeywordAbility.kicker("{4}{B}"))

    triggeredAbility {
        trigger = Triggers.self.isCast()
        interveningIf = Conditions.WasKicked
        effect = Effects.ForEachPlayer(
            players = Player.Each,
            effect = Effects.LoseHalfLife(roundUp = true)
        )
        description = "When you cast this spell, if it was kicked, each player loses half their " +
            "life, rounded up."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "122"
        artist = "Chase Stone"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4dea7bcc-3ff8-40b0-8264-0cd678d6ff31.jpg?1783929370"
        ruling(
            "2020-09-25",
            "An ability that triggers when a player casts a spell resolves before the spell that " +
                "caused it to trigger. It resolves even if that spell is countered. Notably, Scourge " +
                "of the Skyclaves may have its toughness change from less than 0 to greater than 0 as " +
                "a result of its triggered ability before it resolves and enters the battlefield."
        )
        ruling(
            "2020-09-25",
            "The ability that defines Scourge of the Skyclaves's power and toughness applies in all " +
                "zones, not just the battlefield."
        )
        ruling(
            "2020-09-25",
            "If the highest life total among players is greater than 20, Scourge of the Skyclaves " +
                "has negative power and toughness equal to the difference."
        )
    }
}
