package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EntersWithCounters
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Sanctuary Warden (SNC #30) — {4}{W}{W} Creature — Angel Soldier · 5/5
 *
 * Flying
 * This creature enters with two shield counters on it.
 * Whenever this creature enters or attacks, you may remove a counter from a creature or
 * planeswalker you control. If you do, draw a card and create a 1/1 green and white Citizen
 * creature token.
 *
 * The trigger is Mister Hyde's "remove a counter from a creature you control. If you do, draw"
 * shape behind a "you may": the permanent is *chosen* on resolution, not targeted, so it is a
 * non-targeting [Effects.Pipeline] `selectTarget` feeding [Effects.RemoveCounterOfAnyKind] — the
 * player picks which kind comes off (any kind, not just shield; ruling 2022-04-29) — and the
 * payoff hangs off [Effects.IfYouDo] with [SuccessCriterion.CountersRemoved], so nothing is drawn
 * or created unless a counter actually came off. The choice is narrowed to permanents that carry
 * a counter (a counterless one could never pay), and the may-question is skipped entirely when you
 * control none.
 */
val SanctuaryWarden = card("Sanctuary Warden") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Angel Soldier"
    power = 5
    toughness = 5
    oracleText = "Flying\n" +
        "This creature enters with two shield counters on it.\n" +
        "Whenever this creature enters or attacks, you may remove a counter from a creature or " +
        "planeswalker you control. If you do, draw a card and create a 1/1 green and white Citizen " +
        "creature token."

    keywords(Keyword.FLYING)

    replacementEffect(
        EntersWithCounters(
            counterType = CounterType.SHIELD,
            count = 2,
            selfOnly = true,
        )
    )

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = removeCounterToDrawAndMakeCitizen()
        description = TRIGGER_TEXT
    }

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = removeCounterToDrawAndMakeCitizen()
        description = TRIGGER_TEXT
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "30"
        artist = "Johannes Voss"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4dfdab77-d7c3-4e6a-bb4b-55153bd2fd4d.jpg?1783923151"
        ruling("2022-04-29", "Sanctuary Warden's triggered ability allows you to remove any kind of counter, not just a shield counter.")
    }
}

private const val TRIGGER_TEXT = "Whenever this creature enters or attacks, you may remove a counter from a " +
    "creature or planeswalker you control. If you do, draw a card and create a 1/1 green and white " +
    "Citizen creature token."

private fun removeCounterToDrawAndMakeCitizen(): Effect {
    val counterHolderYouControl = GameObjectFilter.CreatureOrPlaneswalker.youControl().withAnyCounter()
    return Effects.May(
        Effects.Pipeline {
            val holder = selectTarget(
                TargetObject(filter = TargetFilter(counterHolderYouControl)),
                nonTargeting = true,
                prompt = "Choose a creature or planeswalker you control to remove a counter from"
            )
            run(Effects.IfYouDo(
                action = Effects.RemoveCounterOfAnyKind(holder.asTarget),
                then = Effects.DrawCards(1) then Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.GREEN, Color.WHITE),
                    creatureTypes = setOf("Citizen"),
                ),
                successCriterion = SuccessCriterion.CountersRemoved,
            ))
        },
        prompt = "Remove a counter from a creature or planeswalker you control to draw a card and create a Citizen?",
        feasibility = FeasibilityCheck.ControlsPermanentMatching(counterHolderYouControl),
    )
}
