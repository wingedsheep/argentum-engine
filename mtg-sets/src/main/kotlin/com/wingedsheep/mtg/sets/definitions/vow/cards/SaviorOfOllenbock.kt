package com.wingedsheep.mtg.sets.definitions.vow.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.training
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Savior of Ollenbock
 * {1}{W}{W}
 * Creature — Human Soldier
 * 1/2
 * Training (Whenever this creature attacks with another creature with greater power, put a
 * +1/+1 counter on this creature.)
 * Whenever this creature trains, exile up to one other target creature from the battlefield
 * or creature card from a graveyard.
 * When this creature leaves the battlefield, put the exiled cards onto the battlefield under
 * their owners' control.
 *
 * Three pieces, one payoff loop:
 *  - [training] gives the keyword + the +1/+1 attack trigger. That trigger is also this card's
 *    own train detector: its counter placement emits the parameterless `TrainedEvent`
 *    (CR 702.149c — fired only when the counter actually lands), which the next ability keys on.
 *  - A "whenever this creature trains" trigger ([Triggers.trains], SELF binding) that exiles
 *    **up to one** target — a cross-zone union (CR 115.1 single target, two zone clauses):
 *    another creature on the battlefield ([TargetFilter.OtherCreature]) OR a creature card in
 *    any graveyard ([TargetFilter.CreatureInGraveyard]), built with [TargetFilter.or]. `optional`
 *    on the [TargetObject] models "up to one" so the trigger can resolve exiling nothing.
 *    [Effects.ExileUntilLeaves] links each exiled object to this permanent — the executor accepts
 *    both battlefield and graveyard sources (the Savior engine relaxation).
 *  - A leaves-the-battlefield trigger returning every linked exiled card under its owner's control
 *    ([Effects.ReturnLinkedExileUnderOwnersControl]). Because each train exiles independently and
 *    all link to the same source, they all come back together when the Savior leaves.
 */
val SaviorOfOllenbock = card("Savior of Ollenbock") {
    manaCost = "{1}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Soldier"
    power = 1
    toughness = 2
    oracleText = "Training (Whenever this creature attacks with another creature with greater " +
        "power, put a +1/+1 counter on this creature.)\n" +
        "Whenever this creature trains, exile up to one other target creature from the " +
        "battlefield or creature card from a graveyard.\n" +
        "When this creature leaves the battlefield, put the exiled cards onto the battlefield " +
        "under their owners' control."

    training()

    triggeredAbility {
        trigger = Triggers.trains()
        val victim = target(
            "up to one other target creature from the battlefield or creature card from a graveyard",
            TargetObject(
                optional = true,
                filter = TargetFilter.OtherCreature.or(TargetFilter.CreatureInGraveyard),
            ),
        )
        effect = Effects.ExileUntilLeaves(victim)
    }

    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "34"
        artist = "Aaron J. Riley"
        imageUri = "https://cards.scryfall.io/normal/front/b/a/ba77e83b-1846-4c42-bea0-2e304429fbe0.jpg?1783924908"

        ruling(
            "2021-11-19",
            "A creature \"trains\" when a +1/+1 counter is put onto it as a result of its training " +
                "ability resolving.",
        )
        ruling(
            "2021-11-19",
            "A creature's training ability triggers only when both that creature and a creature " +
                "with greater power are declared as attackers. Increasing a creature's power after " +
                "attackers are declared won't cause a training ability to trigger.",
        )
        ruling(
            "2021-11-19",
            "Once a creature's training ability has triggered, destroying the other attacking " +
                "creature or reducing its power won't stop the creature with training from getting " +
                "a +1/+1 counter.",
        )
    }
}
