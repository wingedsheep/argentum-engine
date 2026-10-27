package com.wingedsheep.mtg.sets.definitions.gtc.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hellkite Tyrant — Gatecrash #94 (canonical printing)
 * {4}{R}{R} · Creature — Dragon 6/5
 *
 * Flying, trample
 * Whenever this creature deals combat damage to a player, gain control of all artifacts that player controls.
 * At the beginning of your upkeep, if you control twenty or more artifacts, you win the game.
 *
 * "That player" is the damaged player, read through `controlledByTriggeringPlayer()` (Nature's
 * Will's shape); the control change is permanent. The win is an intervening-if trigger, so the
 * artifact count is checked both when the upkeep begins and again on resolution (ruling).
 */
val HellkiteTyrant = card("Hellkite Tyrant") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    power = 6
    toughness = 5
    oracleText = "Flying, trample\n" +
        "Whenever this creature deals combat damage to a player, gain control of all artifacts that player controls.\n" +
        "At the beginning of your upkeep, if you control twenty or more artifacts, you win the game."

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Artifact.controlledByTriggeringPlayer()),
            Effects.GainControl(EffectTarget.IterationEntity, Duration.Permanent)
        )
        description = "Whenever this creature deals combat damage to a player, gain control of all artifacts " +
            "that player controls."
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.UPKEEP)
        interveningIf = Conditions.YouControlAtLeast(20, GameObjectFilter.Artifact)
        effect = Effects.WinGame(
            target = EffectTarget.Controller,
            message = "Hellkite Tyrant: twenty or more artifacts."
        )
        description = "At the beginning of your upkeep, if you control twenty or more artifacts, you win the game."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "94"
        artist = "Aleksi Briclot"
        imageUri = "https://cards.scryfall.io/normal/front/0/b/0bc3401f-935b-45ce-b1e6-300a5d9dfd4f.jpg?1783940124"
        ruling(
            "2013-01-24",
            "If you don't control twenty or more artifacts at the beginning of your upkeep, Hellkite Tyrant's " +
                "last ability won't trigger. If it does trigger, it will check again when it tries to resolve. " +
                "If you don't control twenty or more artifacts at that time, the ability will do nothing."
        )
    }
}
