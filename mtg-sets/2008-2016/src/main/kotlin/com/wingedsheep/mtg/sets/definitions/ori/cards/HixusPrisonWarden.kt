package com.wingedsheep.mtg.sets.definitions.ori.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hixus, Prison Warden — Magic Origins #19
 * {3}{W}{W} · Legendary Creature — Human Soldier · 4/4
 *
 * Flash
 * Whenever a creature deals combat damage to you, if Hixus entered this turn, exile that
 * creature until Hixus leaves the battlefield.
 *
 * - The combat-damage trigger is The Cabbage Merchant's
 *   `Triggers.a(Creature).dealsCombatDamage(Recipient.You)`: it fires once per creature, and the
 *   damage-to-you detector binds the damage *source* as the triggering entity ("that creature").
 * - "if Hixus entered this turn" is an intervening if on `SourceEnteredThisTurn` (Kaito
 *   Shizuki's), checked both when the damage is dealt and on resolution.
 * - The exile is Ossification's [Effects.MoveUntilSourceLeaves] on the triggering creature: the
 *   second one-shot effect of CR 610.3 returns it the moment Hixus leaves, without a trigger on
 *   the stack, and (CR 610.3b) it exiles nothing if Hixus has already left when the trigger
 *   resolves (ruling).
 */
val HixusPrisonWarden = card("Hixus, Prison Warden") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Creature — Human Soldier"
    power = 4
    toughness = 4
    oracleText = "Flash (You may cast this spell any time you could cast an instant.)\n" +
        "Whenever a creature deals combat damage to you, if Hixus entered this turn, exile that " +
        "creature until Hixus leaves the battlefield. (That creature returns under its owner's control.)"

    keywords(Keyword.FLASH)

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature).dealsCombatDamage(Recipient.You)
        interveningIf = Conditions.SourceEnteredThisTurn
        effect = Effects.MoveUntilSourceLeaves(EffectTarget.TriggeringEntity, Zone.EXILE)
        description = "Whenever a creature deals combat damage to you, if Hixus entered this turn, " +
            "exile that creature until Hixus leaves the battlefield."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "19"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d0c21a35-4b70-4de4-93ff-c0ccabdc39e0.jpg?1783938361"
        ruling(
            "2015-06-22",
            "If Hixus leaves the battlefield before its triggered ability resolves, the creature that " +
                "dealt combat damage to you won't be exiled."
        )
        ruling("2015-06-22", "If a creature token is exiled, it ceases to exist. It won't be returned to the battlefield.")
    }
}
