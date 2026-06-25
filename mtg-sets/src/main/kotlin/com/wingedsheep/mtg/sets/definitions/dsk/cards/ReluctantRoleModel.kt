package com.wingedsheep.mtg.sets.definitions.dsk.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * Reluctant Role Model — Duskmourn: House of Horror #26
 * {1}{W} · Creature — Human Survivor · 2/2
 *
 * Survival — At the beginning of your second main phase, if this creature is tapped, put a
 * flying, lifelink, or +1/+1 counter on it.
 * Whenever this creature or another creature you control dies, if it had counters on it, put
 * those counters on up to one target creature.
 *
 * First ability: the "Survival" ability word is flavor only — mechanically this is an
 * intervening-"if" postcombat-main trigger gated on the source being tapped
 * (`Triggers.YourPostcombatMain` + `Conditions.SourceIsTapped`), exactly like the other DSK
 * Survival creatures. "put a flying, lifelink, or +1/+1 counter" is the controller's choice at
 * resolution, modeled as `Effects.ChooseAction` over the three keyword/+1+1 counter kinds, each
 * adding one counter to this creature (`EffectTarget.Self`). Flying and lifelink are keyword
 * counters (CR 122.1b) granting their keyword via the projected keyword-counter map.
 *
 * Second ability: "this creature or another creature you control dies" is
 * `Triggers.YourCreatureDies` (ANY binding, creatures-you-control filter — includes this card
 * itself). The intervening "if it had counters on it" (CR 603.4) is
 * `Conditions.TriggeringEntityHadCounters`, reading the dying creature's last-known total
 * counter count. `Effects.MoveAllLastKnownCounters` puts the same number of each kind of counter
 * the dying creature had onto the chosen "up to one target creature" (any creature, optional).
 */
val ReluctantRoleModel = card("Reluctant Role Model") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Survivor"
    power = 2
    toughness = 2
    oracleText = "Survival — At the beginning of your second main phase, if this creature is " +
        "tapped, put a flying, lifelink, or +1/+1 counter on it.\n" +
        "Whenever this creature or another creature you control dies, if it had counters on it, " +
        "put those counters on up to one target creature."

    // Survival — At the beginning of your second main phase, if this creature is tapped,
    // put a flying, lifelink, or +1/+1 counter on it.
    triggeredAbility {
        trigger = Triggers.YourPostcombatMain
        triggerCondition = Conditions.SourceIsTapped
        effect = Effects.ChooseAction(
            listOf(
                EffectChoice(
                    label = "Flying counter",
                    effect = Effects.AddCounters(Counters.FLYING, 1, EffectTarget.Self),
                ),
                EffectChoice(
                    label = "Lifelink counter",
                    effect = Effects.AddCounters(Counters.LIFELINK, 1, EffectTarget.Self),
                ),
                EffectChoice(
                    label = "+1/+1 counter",
                    effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                ),
            )
        )
        description = "Survival — At the beginning of your second main phase, if this creature " +
            "is tapped, put a flying, lifelink, or +1/+1 counter on it."
    }

    // Whenever this creature or another creature you control dies, if it had counters on it,
    // put those counters on up to one target creature.
    triggeredAbility {
        trigger = Triggers.YourCreatureDies
        triggerCondition = Conditions.TriggeringEntityHadCounters
        target = TargetCreature(optional = true)
        effect = Effects.MoveAllLastKnownCounters(EffectTarget.ContextTarget(0))
        description = "Whenever this creature or another creature you control dies, if it had " +
            "counters on it, put those counters on up to one target creature."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "26"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/4/d/4dde86d6-34a0-4b3b-a46a-d9941501d08c.jpg?1726285956"

        ruling("2024-09-20", "If Reluctant Role Model dies at the same time as one or more other creatures with counters on them, its last ability will trigger for each of those creatures.")
        ruling("2024-09-20", "Reluctant Role Model's last ability doesn't cause you to move counters from the creature that died onto the target creature. Rather, you put the same number of each kind of counter the creature that died had when it died onto the target creature.")
        ruling("2024-09-20", "If the creature that died had -1/-1 counters on it when it died, Reluctant Role Model's last ability will include those as well. This may result in the target creature also dying.")
        ruling("2024-09-20", "If a creature with a survival ability isn't tapped when your second main phase begins, the ability won't trigger at all.")
        ruling("2024-09-20", "If a creature's survival ability triggers but that creature is untapped when the ability begins to resolve, that ability won't do anything.")
    }
}
