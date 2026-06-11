package com.wingedsheep.mtg.sets.definitions.tmt.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.SneakCostWasPaid
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/**
 * Turncoat Kunoichi
 * {2}{W}
 * Creature — Mutant Ninja Fox
 * 3/2
 *
 * Sneak {2}{W}{B} (You may cast this spell for {2}{W}{B} if you also return an
 * unblocked attacker you control to hand during the declare blockers step. It
 * enters tapped and attacking.)
 * When this creature enters, choose target creature an opponent controls. Exile
 * that creature until this creature leaves the battlefield. If this creature's
 * sneak cost was paid, instead exile the chosen creature.
 */
val TurncoatKunoichi = card("Turncoat Kunoichi") {
    manaCost = "{2}{W}"
    colorIdentity = "WB"
    typeLine = "Creature — Mutant Ninja Fox"
    oracleText = "Sneak {2}{W}{B} (You may cast this spell for {2}{W}{B} if you also return an unblocked attacker you control to hand during the declare blockers step. It enters tapped and attacking.)\nWhen this creature enters, choose target creature an opponent controls. Exile that creature until this creature leaves the battlefield. If this creature's sneak cost was paid, instead exile the chosen creature."
    power = 3
    toughness = 2

    sneak("{2}{W}{B}")

    // ETB: exile the chosen creature. With the sneak cost paid the exile is permanent;
    // otherwise it returns when Turncoat Kunoichi leaves (the LTB trigger below).
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val creature = target("target creature an opponent controls", Targets.CreatureOpponentControls)
        effect = ConditionalEffect(
            condition = SneakCostWasPaid,
            effect = Effects.Exile(creature),
            elseEffect = Effects.ExileUntilLeaves(creature)
        )
    }
    // LTB: return any creature exiled "until this leaves" (no-op when it was exiled permanently).
    triggeredAbility {
        trigger = Triggers.LeavesBattlefield
        effect = Effects.ReturnLinkedExileUnderOwnersControl()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "26"
        artist = "Manuel Castañón"
        imageUri = "https://cards.scryfall.io/normal/front/3/a/3ae61336-a4ee-40cd-9a18-392d96b873a4.jpg?1769685362"
    }
}
