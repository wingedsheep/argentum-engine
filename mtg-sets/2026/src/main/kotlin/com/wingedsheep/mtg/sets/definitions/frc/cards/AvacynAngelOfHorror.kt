package com.wingedsheep.mtg.sets.definitions.frc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

private const val AVACYN_RETURN_TEXT =
    "Whenever Avacyn or another nontoken creature you control dies, return that card to the " +
        "battlefield under your control at the beginning of the next end step."

/**
 * Avacyn, Angel of Horror
 * {5}{B}{B}{B}
 * Legendary Creature — Angel
 * 8/8
 *
 * Flying, deathtouch
 * Whenever Avacyn or another nontoken creature you control dies, return that card to the
 * battlefield under your control at the beginning of the next end step.
 *
 * "Avacyn or another" is two triggers, as on Pawn of Ulamog: the self half reads the dying
 * Avacyn, the "another" half the other nontoken creature. Each schedules a delayed trigger for the
 * next end step (any player's) that returns the card from the graveyard under the ability
 * controller's control; the graveyard guard makes the return a no-op if the card left the
 * graveyard in the meantime.
 */
val AvacynAngelOfHorror = card("Avacyn, Angel of Horror") {
    manaCost = "{5}{B}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Angel"
    power = 8
    toughness = 8
    oracleText = "Flying, deathtouch\n$AVACYN_RETURN_TEXT"

    keywords(Keyword.FLYING, Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Effects.CreateDelayedTrigger(
            step = Step.END,
            effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self, underYourControl = true),
        )
        description = AVACYN_RETURN_TEXT
    }

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature.nontoken().youControl()).dies()
        effect = Effects.CreateDelayedTrigger(
            step = Step.END,
            effect = Effects.PutOntoBattlefieldFromGraveyard(
                EffectTarget.TriggeringEntity,
                underYourControl = true,
            ),
        )
        description = AVACYN_RETURN_TEXT
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "7"
        artist = "Pauline Voss"
        flavorText = "\"She binds them to do naught but worship her. It is no salvation she grants, " +
            "but servitude.\"\n—Geist of Saint Thalia"
        imageUri = "https://cards.scryfall.io/normal/front/f/f/ff65f46b-0f82-49b8-93b9-7ac0243ce941.jpg?1789644252"
        ruling(
            "2026-08-21",
            "If Avacyn dies at the same time as another nontoken creature, her ability will trigger for each of them.",
        )
    }
}
