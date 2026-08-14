package com.wingedsheep.mtg.sets.definitions.mrd.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Woebearer — Mirrodin #83
 * {4}{B} · Creature — Zombie · 2/3
 *
 * Fear
 * Whenever this creature deals combat damage to a player, you may return target creature card
 * from your graveyard to your hand.
 *
 * Modelling notes:
 * - Fear is a printed evasion keyword ([Keyword.FEAR]) — "can't be blocked except by artifact
 *   creatures and/or black creatures" — so Woebearer connects often enough for the recursion
 *   trigger to matter.
 * - The trigger targets on the way onto the stack but the return is a "may", so `optional = true`
 *   sits on the ability while the target requirement stays mandatory: a legal creature card in
 *   your graveyard must be chosen for the trigger to go on the stack at all, and only then can
 *   the controller decline. If that card leaves the graveyard in response, the trigger fizzles.
 * - "your graveyard" is the *controller's* graveyard at resolution, which is what
 *   [TargetFilter.CreatureInYourGraveyard] models — an opponent's graveyard is never a source.
 */
val Woebearer = card("Woebearer") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Zombie"
    power = 2
    toughness = 3
    oracleText = "Fear (This creature can't be blocked except by artifact creatures and/or " +
        "black creatures.)\n" +
        "Whenever this creature deals combat damage to a player, you may return target creature " +
        "card from your graveyard to your hand."

    keywords(Keyword.FEAR)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        optional = true
        val creatureCard = target(
            "target creature card",
            TargetObject(filter = TargetFilter.CreatureInYourGraveyard)
        )
        effect = Effects.Move(creatureCard, Zone.HAND)
        description = "Whenever this creature deals combat damage to a player, you may return " +
            "target creature card from your graveyard to your hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "83"
        artist = "Matt Thompson"
        imageUri = "https://cards.scryfall.io/normal/front/c/5/c5a6e960-169d-431a-9ebd-c4413aa67377.jpg?1783944543"
    }
}
