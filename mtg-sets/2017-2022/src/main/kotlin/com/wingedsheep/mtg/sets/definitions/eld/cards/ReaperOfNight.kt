package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Reaper of Night // Harvest Fear
 * {5}{B}{B}
 * Creature — Specter
 * 4/5
 * Whenever this creature attacks, if defending player has two or fewer cards in hand, it gains
 * flying until end of turn.
 *
 * Adventure: Harvest Fear — {3}{B}, Sorcery — Adventure
 * Target opponent discards two cards.
 *
 * The intervening-if (CR 603.4) counts the hand of `Player.DefendingPlayer`, which resolves
 * through this creature's own attack assignment, so in multiplayer it is the player it attacks.
 */
val ReaperOfNight = card("Reaper of Night") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Specter"
    power = 4
    toughness = 5
    oracleText = "Whenever this creature attacks, if defending player has two or fewer cards in hand, " +
        "it gains flying until end of turn."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.count(Player.DefendingPlayer, Zone.HAND),
            ComparisonOperator.LTE,
            2
        )
        effect = Effects.GrantKeyword(Keyword.FLYING, EffectTarget.Self)
    }

    adventure("Harvest Fear") {
        manaCost = "{3}{B}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Target opponent discards two cards. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            val opponent = target(Targets.Opponent)
            effect = Patterns.Hand.discardCards(2, opponent)
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "102"
        artist = "Jeff Simpson"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4dc774b4-3f70-4351-b1b8-8a0193cb3a50.jpg?1783932635"
    }
}
