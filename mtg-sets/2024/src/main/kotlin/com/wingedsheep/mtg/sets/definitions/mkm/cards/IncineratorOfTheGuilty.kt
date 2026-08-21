package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/** Pipeline slot the chosen X rides from the collect-evidence action to the reflexive payoff. */
private const val EVIDENCE_X = "incineratorEvidenceX"

/**
 * Incinerator of the Guilty — Murders at Karlov Manor #132
 * {4}{R}{R} · Creature — Dragon · 6/6
 *
 * Flying, trample
 * Whenever this creature deals combat damage to a player, you may collect evidence X. When you do,
 * this creature deals X damage to each creature and planeswalker that player controls.
 *
 * **X is chosen, not derived.** [Effects.CollectEvidenceChosenAmount] asks the controller for X
 * (bounded at their graveyard's total mana value, so CR 701.59b is enforced on the *choice*) and
 * only then which cards to exile. The tempting shortcut — let the player exile whatever they like
 * and set X to the total — is wrong, because over-exiling is legal (CR 701.59a) and would silently
 * push X above what they picked.
 *
 * X = 0 is a legal collection that exiles nothing and still counts as having collected evidence
 * (2024-02-02 ruling), so "whenever you collect evidence" payoffs still see it — and the "may" is
 * therefore always offered, however thin the graveyard.
 *
 * "When you do" is a genuine reflexive trigger (CR 603.12): it goes on the stack after the
 * collection resolves and opponents may respond to it. X survives that round-trip as a pipeline
 * stored number, which is why the payoff reads it as [DynamicAmount.VariableReference] rather than
 * recomputing anything.
 *
 * "That player" is the player just damaged, hence `controlledByTriggeringPlayer()` rather than
 * "each opponent" — the two coincide only in a two-player game. The damage is untargeted (it hits
 * *each* such permanent), so the reflexive half declares no target requirements. Leaving
 * `damageSource` unset attributes it to this creature, which matters for "deals damage" payoffs
 * and for lifelink.
 */
val IncineratorOfTheGuilty = card("Incinerator of the Guilty") {
    manaCost = "{4}{R}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dragon"
    oracleText = "Flying, trample\n" +
        "Whenever this creature deals combat damage to a player, you may collect evidence X. " +
        "When you do, this creature deals X damage to each creature and planeswalker that player " +
        "controls. (To collect evidence X, exile cards with total mana value X or greater from " +
        "your graveyard.)"
    power = 6
    toughness = 6

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.DealsCombatDamageToPlayer
        effect = ReflexiveTriggerEffect(
            action = Effects.CollectEvidenceChosenAmount(EVIDENCE_X),
            optional = true,
            reflexiveEffect = Patterns.Group.dealDamageToAll(
                DynamicAmount.VariableReference(EVIDENCE_X),
                GroupFilter(GameObjectFilter.CreatureOrPlaneswalker.controlledByTriggeringPlayer()),
            ),
            descriptionOverride = "You may collect evidence X. When you do, this creature deals X " +
                "damage to each creature and planeswalker that player controls.",
        )
        description = "Whenever this creature deals combat damage to a player, you may collect " +
            "evidence X. When you do, this creature deals X damage to each creature and " +
            "planeswalker that player controls."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "132"
        artist = "Lucas Graciano"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c6aca64-a554-45c1-9f23-4f7878abeda5.jpg?1783912888"
    }
}
