package com.wingedsheep.mtg.sets.definitions.bfz.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility

/**
 * Encircling Fissure
 * {2}{W}
 * Instant
 * Prevent all combat damage that would be dealt this turn by creatures target opponent controls.
 * Awaken 2—{4}{W} (If you cast this spell for {4}{W}, also put two +1/+1 counters on target land
 * you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)
 *
 * The prevention shield pins the targeted opponent as the spell resolves, while which creatures
 * that player controls is checked each time combat damage would be dealt — so it also covers a
 * creature they control that wasn't on the battlefield (or wasn't a creature) at resolution.
 */
val EncirclingFissure = card("Encircling Fissure") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Prevent all combat damage that would be dealt this turn by creatures target opponent controls.\n" +
        "Awaken 2—{4}{W} (If you cast this spell for {4}{W}, also put two +1/+1 counters on target " +
        "land you control and it becomes a 0/0 Elemental creature with haste. It's still a land.)"

    keywordAbility(KeywordAbility.awaken(2, "{4}{W}"))

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.PreventCombatDamageFrom(
            source = GameObjectFilter.Creature.targetPlayerControls(opponent),
            duration = Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "23"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/5/7/575da7cb-6715-43df-a7b4-a4ca00216a1d.jpg?1783938221"
        ruling(
            "2015-08-25",
            "Encircling Fissure prevents all combat damage that would be dealt by any creature controlled by " +
                "the target opponent, even if that creature wasn't on the battlefield or wasn't a creature as " +
                "Encircling Fissure resolved."
        )
    }
}
