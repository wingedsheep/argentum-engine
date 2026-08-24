package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.effects.CantBeRegeneratedEffect
import com.wingedsheep.sdk.scripting.effects.DamageToTargetCantBePreventedThisTurnEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Whippoorwill
 * {G}
 * Creature — Bird
 * 1/1
 * {G}{G}, {T}: Target creature can't be regenerated this turn. Damage that would be dealt to that
 * creature this turn can't be prevented or dealt instead to another permanent or player. When the
 * creature dies this turn, exile the creature.
 *
 * Three clauses that between them close every escape a creature has from dying and staying dead:
 * regeneration, damage prevention/redirection, and the graveyard.
 *
 * The middle one is the interesting piece. "Can't be prevented" is a rules modification (CR 615.9),
 * not a replacement effect, so it can't compete in the replacement gather — it has to be consulted
 * where prevention is *applied*. It is therefore a marker on the recipient, read by
 * `DamageUtils.isDamagePreventionDisabled(state, recipientId)`; the same marker also suppresses
 * redirection, which is the "or dealt instead to another permanent or player" half. Scoped to the
 * one creature, not global — the global form would blank every prevention effect in the game.
 *
 * The last clause is a genuine replacement, granted for the turn: battlefield→graveyard becomes
 * battlefield→exile. Deliberately not `GrantExileOnLeave`, which fires on *any* departure — a
 * Whippoorwilled creature that is bounced or exiled some other way is untouched by this card.
 */
val Whippoorwill = card("Whippoorwill") {
    manaCost = "{G}"
    colorIdentity = "G"
    typeLine = "Creature — Bird"
    power = 1
    toughness = 1
    oracleText = "{G}{G}, {T}: Target creature can't be regenerated this turn. Damage that would " +
        "be dealt to that creature this turn can't be prevented or dealt instead to another " +
        "permanent or player. When the creature dies this turn, exile the creature."

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{G}{G}"), Costs.Tap)
        target = Targets.Creature
        effect = Effects.Composite(
            CantBeRegeneratedEffect(EffectTarget.ContextTarget(0)),
            DamageToTargetCantBePreventedThisTurnEffect(EffectTarget.ContextTarget(0)),
            Effects.GrantReplacementEffect(
                replacement = RedirectZoneChange(
                    newDestination = Zone.EXILE,
                    appliesTo = EventPattern.ZoneChangeEvent(
                        filter = GameObjectFilter.Creature,
                        from = Zone.BATTLEFIELD,
                        to = Zone.GRAVEYARD,
                    ),
                ),
                target = EffectTarget.ContextTarget(0),
                duration = Duration.EndOfTurn,
            ),
        )
        description = "{G}{G}, {T}: Target creature can't be regenerated this turn. Damage that " +
            "would be dealt to that creature this turn can't be prevented or dealt instead to " +
            "another permanent or player. When the creature dies this turn, exile the creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "91"
        artist = "Douglas Shuler"
        flavorText = "If the Whippoorwill remains silent, the soul has not reached its reward."
        imageUri = "https://cards.scryfall.io/normal/front/e/5/e56146bf-5db0-4bef-83bb-efa5ebec6684.jpg?1783947929"
    }
}
