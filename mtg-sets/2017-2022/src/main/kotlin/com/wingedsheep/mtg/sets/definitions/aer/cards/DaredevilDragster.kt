package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Daredevil Dragster
 * {3}
 * Artifact — Vehicle
 * 4/4
 * At end of combat, if this Vehicle attacked or blocked this combat, put a velocity counter on it.
 * Then if it has two or more velocity counters on it, sacrifice it and draw two cards.
 * Crew 2
 *
 * The end-of-combat trigger fires on any player's turn (blocking counts) and is gated by the
 * per-combat attacked/blocked markers as an intervening "if". The two-counter check is made only
 * as the trigger resolves (`Effects.If`), so a second counter put on another way doesn't sacrifice
 * it yet; if the Dragster left the battlefield with the trigger on the stack, the count is read
 * from last-known information — two counters still draws two cards, one never gains a second.
 * "Sacrifice it and draw two cards" are independent: the draw doesn't hinge on the sacrifice.
 */
val DaredevilDragster = card("Daredevil Dragster") {
    manaCost = "{3}"
    colorIdentity = ""
    typeLine = "Artifact — Vehicle"
    power = 4
    toughness = 4
    oracleText = "At end of combat, if this Vehicle attacked or blocked this combat, put a velocity counter " +
        "on it. Then if it has two or more velocity counters on it, sacrifice it and draw two cards.\n" +
        "Crew 2 (Tap any number of creatures you control with total power 2 or more: This Vehicle becomes " +
        "an artifact creature until end of turn.)"

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.END_COMBAT)
        interveningIf = Conditions.SourceAttackedOrBlockedThisCombat
        effect = Effects.AddCounters(CounterType.VELOCITY, 1, EffectTarget.Self) then
            Effects.If(
                condition = Conditions.SourceCounterCountAtLeast(CounterType.VELOCITY, 2),
                then = Effects.SacrificeTarget(EffectTarget.Self) then Effects.DrawCards(2),
            )
        description = "At end of combat, if this Vehicle attacked or blocked this combat, put a velocity " +
            "counter on it. Then if it has two or more velocity counters on it, sacrifice it and draw two cards."
    }

    keywordAbility(KeywordAbility.crew(2))

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "149"
        artist = "Titus Lunter"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/19bbbc98-0806-4226-8fd1-16b812334cee.jpg?1783936730"
        ruling("2017-02-09", "If a second velocity counter is put on Daredevil Dragster by something other than the resolution of its triggered ability, you won't sacrifice it yet.")
        ruling("2017-02-09", "If Daredevil Dragster has one velocity counter on it and leaves the battlefield while its triggered ability is on the stack, it won't get a second velocity counter and you won't draw cards.")
        ruling("2017-02-09", "If Daredevil Dragster somehow has two velocity counters on it and leaves the battlefield while its triggered ability is on the stack, you'll draw two cards even though you can't sacrifice it.")
        ruling("2017-02-09", "If Daredevil Dragster doesn't survive the combat damage step, its ability won't trigger.")
        ruling("2017-09-29", "For a Vehicle to be able to attack, it must be a creature as the declare attackers step begins, so the latest you can activate its crew ability to attack with it is during the beginning of combat step. For a Vehicle to be able to block, it must be a creature as the declare blockers step begins, so the latest you can activate its crew ability to block with it is during the declare attackers step. In either case, players may take actions after the crew ability resolves but before the Vehicle has been declared as an attacking or blocking creature.")
        ruling("2017-09-29", "Each Vehicle is printed with a power and toughness, but it's not a creature. If it becomes a creature (most likely through its crew ability), it will have that power and toughness.")
        ruling("2017-09-29", "Vehicle is an artifact type, not a creature type. A Vehicle that's crewed won't normally have any creature type.")
        ruling("2017-09-29", "Any untapped creature you control can be tapped to pay a crew cost, even one that just came under your control.")
        ruling("2017-09-29", "You may tap more creatures than necessary to activate a crew ability.")
    }
}
