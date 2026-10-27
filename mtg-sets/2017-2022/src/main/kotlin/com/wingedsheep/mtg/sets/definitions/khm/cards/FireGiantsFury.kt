package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.events.Recipient
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Fire Giant's Fury
 * {1}{R}
 * Sorcery
 * Target Giant you control gets +2/+2 and gains trample until end of turn. Whenever it deals combat
 * damage to a player this turn, exile that many cards from the top of your library. Until the end
 * of your next turn, you may play those cards.
 *
 * "Whenever it deals combat damage to a player this turn" is a delayed triggered ability created by
 * the spell (CR 603.7), not an ability granted to the Giant: it watches the targeted creature, is
 * controlled by the spell's caster — so "your library" and the play permission stay with the caster
 * even if another player gains control of the Giant — and may fire more than once (double strike).
 * "That many" is the combat damage the trigger saw. Cast at sorcery speed on your own turn, "until
 * the end of your next turn" is the cleanup step of your following turn.
 */
val FireGiantsFury = card("Fire Giant's Fury") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Target Giant you control gets +2/+2 and gains trample until end of turn. Whenever it " +
        "deals combat damage to a player this turn, exile that many cards from the top of your library. " +
        "Until the end of your next turn, you may play those cards."

    spell {
        val giant = target(TargetFilter(GameObjectFilter.Creature.withSubtype("Giant").youControl()))
        effect = Effects.ModifyStats(2, 2, giant) then
            Effects.GrantKeyword(Keyword.TRAMPLE, giant) then
            Effects.CreateDelayedTrigger(
                effect = Patterns.Exile.impulse(
                    count = DynamicAmounts.triggerDamageAmount(),
                    expiry = MayPlayExpiry.UntilControllerStep(Step.CLEANUP, includeCurrentTurn = false)
                ),
                trigger = Triggers.self.dealsCombatDamage(Recipient.AnyPlayer),
                watchedTarget = giant,
                expiry = DelayedTriggerExpiry.EndOfTurn
            )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "389"
        artist = "Andrew Mar"
        flavorText = "So ended the feast, the hall, and the clan."
        imageUri = "https://cards.scryfall.io/normal/front/c/7/c7f05d49-dfc1-4dad-89e0-07174330d98e.jpg?1783928116"
        ruling("2021-02-05", "You may play those cards that turn even if the Giant leaves the battlefield or another player gains control of it.")
        ruling("2021-02-05", "Any exiled cards that you don't play by the end of your next turn remain exiled.")
    }
}
