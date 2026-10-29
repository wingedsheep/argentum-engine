package com.wingedsheep.mtg.sets.definitions.cns.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.events.AttackPredicate
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Scourge of the Throne
 * {4}{R}{R}
 * Creature — Dragon
 * 5/5
 * Flying
 * Dethrone (Whenever this creature attacks the player with the most life or tied for most life,
 * put a +1/+1 counter on it.)
 * Whenever this creature attacks for the first time each turn, if it's attacking the player with
 * the most life or tied for most life, untap all attacking creatures. After this phase, there is an
 * additional combat phase.
 *
 * Dethrone (CR 702.105) is its reminder-text trigger: checked only as it triggers
 * (`triggerRestriction`), and attacking a planeswalker or battle doesn't count
 * (`DefenderIsPlayer`). The extra-combat trigger instead carries an intervening "if" (CR 603.4),
 * so the attacked player's life standing is checked again on resolution. Its attack target is
 * fixed at declaration, so "attacking a player" sits on the trigger itself: a first attack at a
 * planeswalker never triggers, and a later attack that turn isn't the first.
 */
val ScourgeOfTheThrone = card("Scourge of the Throne") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "Dethrone (Whenever this creature attacks the player with the most life or tied for most life, " +
        "put a +1/+1 counter on it.)\n" +
        "Whenever this creature attacks for the first time each turn, if it's attacking the player with " +
        "the most life or tied for most life, untap all attacking creatures. After this phase, there is " +
        "an additional combat phase."

    keywords(Keyword.FLYING)

    // Dethrone
    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.DefenderIsPlayer))
        triggerRestriction = Conditions.PlayerHasMostLife(Player.DefendingPlayer)
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
    }

    triggeredAbility {
        trigger = Triggers.self.attacks(setOf(AttackPredicate.FirstTimeEachTurn, AttackPredicate.DefenderIsPlayer))
        interveningIf = Conditions.PlayerHasMostLife(Player.DefendingPlayer)
        effect = Effects.ForEachInGroup(
            GroupFilter.AttackingCreatures,
            Effects.Untap(EffectTarget.IterationEntity)
        ) then Effects.AddCombatPhase
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "35"
        artist = "Michael Komarck"
        imageUri = "https://cards.scryfall.io/normal/front/a/0/a01f829b-22aa-4988-9698-5e1ebceb1dd8.jpg?1783939374"
        ruling(
            "2014-05-29",
            "Scourge of the Throne's ability has an intervening \"if\" clause. It must be attacking the player " +
                "with the most life or tied with the most life both when it's declared as an attacker and as it " +
                "starts to resolve for it to have any effect. If, at either time, the player isn't the one with " +
                "the most life or tied for the most life, the ability will have no effect. Notably, this is " +
                "different than how dethrone works. (Dethrone checks only once to see if the ability triggers.)"
        )
        ruling(
            "2014-05-29",
            "Once dethrone triggers, it doesn't matter what happens to the players' life totals before the " +
                "ability resolves. You'll put a +1/+1 counter on the creature even if the defending player " +
                "doesn't have the most life as the ability resolves."
        )
        ruling(
            "2023-07-28",
            "Dethrone doesn't trigger if the creature attacks a planeswalker, even if its controller has the " +
                "most life. The same is true if the creature attacks a battle, even if its protector has the most life."
        )
    }
}
