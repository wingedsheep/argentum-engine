package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.effects.GrantTriggeredAbilityEffect
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Not Dead After All
 * {B}
 * Instant
 *
 * Until end of turn, target creature you control gains "When this creature dies, return it to the
 * battlefield tapped under its owner's control, then create a Wicked Role token attached to it."
 *
 * Same shape as Undying Malice — a SELF-bound dies trigger granted for the turn — with a Wicked
 * Role in place of the +1/+1 counter. The return is gated on `fromZone = GRAVEYARD` so it no-ops
 * if the card already left the graveyard, and `MoveToZoneEffect` returns under the owner's control
 * by default. The graveyard → battlefield return keeps the same entity id, so `EffectTarget.Self`
 * in the follow-up [Effects.CreateRoleToken] lands on the returned permanent. That ordering also
 * gives the card's ruling for free: if the creature doesn't come back (or comes back as a
 * noncreature), the Role attach finds no legal creature and no token is created.
 */
val NotDeadAfterAll = card("Not Dead After All") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Instant"
    oracleText = "Until end of turn, target creature you control gains \"When this creature dies, " +
        "return it to the battlefield tapped under its owner's control, then create a Wicked Role " +
        "token attached to it.\" (If you control another Role on it, put that one into the graveyard. " +
        "Enchanted creature gets +1/+1. When this token is put into a graveyard, each opponent loses 1 life.)"

    spell {
        val t = target("target", Targets.CreatureYouControl)
        effect = GrantTriggeredAbilityEffect(
            ability = TriggeredAbility.create(
                trigger = Triggers.Dies.event,
                binding = Triggers.Dies.binding,
                effect = Effects.Composite(
                    Effects.Move(
                        target = EffectTarget.Self,
                        destination = Zone.BATTLEFIELD,
                        placement = ZonePlacement.Tapped,
                        fromZone = Zone.GRAVEYARD
                    ),
                    Effects.CreateRoleToken("Wicked Role", EffectTarget.Self)
                ),
                descriptionOverride = "When this creature dies, return it to the battlefield tapped " +
                    "under its owner's control, then create a Wicked Role token attached to it."
            ),
            target = t,
            duration = Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "101"
        artist = "Randy Vargas"
        imageUri = "https://cards.scryfall.io/normal/front/d/0/d01a2b68-efe6-4027-846d-db7b19d9eef6.jpg?1783915104"
        ruling("2023-09-01", "If the creature doesn't return to the battlefield or returns as a noncreature permanent, the Wicked Role token won't be created.")
        ruling(
            "2023-09-01",
            "If a permanent has more than one Role attached to it controlled by the same player, each of " +
                "those Roles except the one with the most recent timestamp is put into its owner's graveyard. " +
                "This is a state-based action."
        )
    }
}
