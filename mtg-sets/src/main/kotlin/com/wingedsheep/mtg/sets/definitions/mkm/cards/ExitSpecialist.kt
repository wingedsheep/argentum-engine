package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlockedBy
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Exit Specialist — Murders at Karlov Manor #55
 * {1}{U} · Creature — Human Detective · 2/1
 *
 * This creature can't be blocked by creatures with power 3 or greater.
 * Disguise {1}{U}
 * When this creature is turned face up, return another target creature to its owner's hand.
 *
 * Two mana down, two mana up: cast it face down for {3}, then flip for {1}{U} at the moment an
 * attacker or blocker is committed and bounce it. Because turning face up is a special action that
 * can't be responded to (CR 701.34a), the only window an opponent gets is after the trigger is
 * already on the stack with its target chosen.
 *
 * "Another target creature" is [TargetFilter.OtherCreature] — Exit Specialist can never bounce
 * itself, and the trigger has no legal target (and so is removed from the stack) if it is the only
 * creature on the battlefield when the flip happens.
 *
 * The evasion clause reads *blocker* power at declare-blockers, not continuously: per the Scryfall
 * ruling below, pumping a blocker to power 3 after blocks are declared doesn't undo the block. That
 * falls out of [CantBeBlockedBy] being enforced at block declaration, so nothing extra is needed
 * here — and equally, flipping Exit Specialist face up after it's already blocked doesn't unblock
 * it, since the restriction never re-applies mid-combat.
 */
val ExitSpecialist = card("Exit Specialist") {
    manaCost = "{1}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human Detective"
    oracleText = "This creature can't be blocked by creatures with power 3 or greater.\n" +
        "Disguise {1}{U} (You may cast this card face down for {3} as a 2/2 creature with ward " +
        "{2}. Turn it face up any time for its disguise cost.)\n" +
        "When this creature is turned face up, return another target creature to its owner's hand."
    power = 2
    toughness = 1

    disguise = "{1}{U}"

    staticAbility {
        ability = CantBeBlockedBy(GameObjectFilter.Creature.powerAtLeast(3))
    }

    triggeredAbility {
        trigger = Triggers.TurnedFaceUp
        val creature = target(
            "another target creature",
            TargetCreature(filter = TargetFilter.OtherCreature)
        )
        effect = Effects.ReturnToHand(creature)
        description = "When this creature is turned face up, return another target creature to " +
            "its owner's hand."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "55"
        artist = "Mila Pesic"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/268f142d-9fb1-4673-b804-add1f08dacb9.jpg?1783912910"

        ruling(
            "2024-02-02",
            "Once Exit Specialist has been blocked, increasing the blocking creature's power to 3 " +
                "or greater won't cause Exit Specialist to become unblocked."
        )
        ruling(
            "2024-02-02",
            "If Exit Specialist becomes blocked while it's face-down, turning it face up won't " +
                "cause it to become unblocked."
        )
        ruling(
            "2024-02-02",
            "Any time you have priority, you may turn the face-down creature face up by revealing " +
                "what its disguise cost is and paying that cost. This is a special action. It " +
                "doesn't use the stack and can't be responded to. Only a face-down permanent can " +
                "be turned face up this way; a face-down spell cannot."
        )
    }
}
