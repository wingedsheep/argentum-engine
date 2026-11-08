package com.wingedsheep.mtg.sets.definitions.c19.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.madness
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.EffectChoice
import com.wingedsheep.sdk.scripting.effects.FeasibilityCheck
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Archfiend of Spite — Commander 2019 #14
 * {5}{B}{B} · Creature — Demon · 6/6
 *
 * Flying
 * Whenever a source an opponent controls deals damage to this creature, that source's controller
 * loses that much life unless they sacrifice that many permanents of their choice.
 * Madness {3}{B}{B}
 *
 * Phyrexian Obliterator's join (`isDealtDamage(by = …)` binds the damage *source* as the triggering
 * entity, so `Player.ControllerOfTriggeringEntity` is "that source's controller" — last-known for a
 * burn spell already off the stack) filtered to opponent-controlled sources, feeding a punisher
 * choice. The sacrifice option's feasibility is `ControlsPermanentMatching(dynamicCount = damage)`,
 * so a controller with fewer than N permanents is only offered the life loss (ruling 2019-08-23).
 * The two choices are all-or-nothing: they sacrifice N permanents or lose N life, never a mix.
 */
val ArchfiendOfSpite = card("Archfiend of Spite") {
    manaCost = "{5}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    power = 6
    toughness = 6
    oracleText = "Flying\n" +
        "Whenever a source an opponent controls deals damage to this creature, that source's controller " +
        "loses that much life unless they sacrifice that many permanents of their choice.\n" +
        "Madness {3}{B}{B} (If you discard this card, discard it into exile. When you do, cast it for " +
        "its madness cost or put it into your graveyard.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.isDealtDamage(by = GameObjectFilter.Any.opponentControls())
        val damage = DynamicAmounts.triggerDamageAmount()
        val sourceController = EffectTarget.PlayerRef(Player.ControllerOfTriggeringEntity)
        effect = Effects.ChooseAction(
            player = sourceController,
            choices = listOf(
                EffectChoice(
                    label = "Sacrifice that many permanents",
                    effect = Effects.Sacrifice(GameObjectFilter.Any, damage, sourceController),
                    feasibilityCheck = FeasibilityCheck.ControlsPermanentMatching(
                        GameObjectFilter.Any,
                        dynamicCount = damage
                    )
                ),
                EffectChoice(
                    label = "Lose that much life",
                    effect = Effects.LoseLife(damage, sourceController)
                )
            )
        )
        description = "Whenever a source an opponent controls deals damage to this creature, that " +
            "source's controller loses that much life unless they sacrifice that many permanents of their choice."
    }

    madness("{3}{B}{B}")

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "14"
        artist = "Tomasz Jedruszek"
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1df2ca13-4062-4827-ae2f-c61cf88ba3b3.jpg?1783932810"
        ruling("2019-08-23", "The source's controller must either sacrifice permanents or lose life. They can't choose to sacrifice some permanents and lose some life.")
        ruling("2019-08-23", "If the player doesn't control enough permanents that they can sacrifice, they must lose life.")
        ruling("2019-08-23", "The player may always choose not to sacrifice permanents, even if they will end up losing more life than they have or if an effect (such as that of Platinum Emperion) says that they can't lose life.")
        ruling("2019-08-23", "The player who loses life or permanents is the player who controls the source as Archfiend of Spite's triggered ability resolves. In some rare cases, this may not be the same player who controlled the source as it dealt damage. If the source is no longer in the zone it was in as it dealt damage, the source's last known information is used to determine who controlled it.")
        ruling("2019-08-23", "Archfiend of Spite's triggered ability will trigger even if it is dealt lethal damage. For example, if Archfiend of Spite blocks a 7/7 creature, its ability will trigger when combat damage is dealt and the attacking player will sacrifice seven permanents or lose 7 life.")
    }
}
