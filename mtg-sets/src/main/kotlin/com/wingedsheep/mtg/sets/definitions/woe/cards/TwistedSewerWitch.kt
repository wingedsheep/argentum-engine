package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Twisted Sewer-Witch
 * {3}{B}{B}
 * Creature — Human Warlock
 * 3/4
 *
 * When this creature enters, create a 1/1 black Rat creature token with "This creature can't block."
 * Then for each Rat you control, create a Wicked Role token attached to that Rat.
 *
 * Two sequenced steps, not one: [woeRatToken] first, *then* the iteration. Because an
 * `IterationSpace.Group` is snapshotted when the `ForEach` starts executing — after the `.then()`
 * boundary — the Rat that was just created is already on the battlefield and is included, which is
 * how the card is meant to read ("for each Rat you control" is evaluated at that point, not before
 * the token entered).
 *
 * Nothing here targets, so the Roles land on Rats with hexproof or shroud too, and the ability can't
 * fizzle. The Rats themselves are the iteration entities, so the body attaches to
 * [EffectTarget.Self] — under a `Group` space that resolves to the current entity rather than to the
 * Witch. [Effects.CreateRoleToken] already implements the Role state-based action (CR 303.7a /
 * 704.5y) that bins an older Role you control on the same creature, so landing this on Rats that
 * already carry a Monster or Cursed Role replaces rather than stacks.
 */
val TwistedSewerWitch = card("Twisted Sewer-Witch") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Human Warlock"
    power = 3
    toughness = 4
    oracleText = "When this creature enters, create a 1/1 black Rat creature token with " +
        "\"This creature can't block.\" Then for each Rat you control, create a Wicked Role token " +
        "attached to that Rat. (If you control another Role on it, put that one into the graveyard. " +
        "Enchanted creature gets +1/+1. When this token is put into a graveyard, each opponent " +
        "loses 1 life.)"

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = woeRatToken().then(
            Effects.ForEachInGroup(
                filter = GroupFilter(GameObjectFilter.Creature.withSubtype("Rat").youControl()),
                effect = Effects.CreateRoleToken("Wicked Role", EffectTarget.Self),
            )
        )
        description = "When this creature enters, create a 1/1 black Rat creature token with " +
            "\"This creature can't block.\" Then for each Rat you control, create a Wicked Role " +
            "token attached to that Rat."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "114"
        artist = "Scott Murphy"
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d6e3ddf7-582d-4923-be30-8428e52237e4.jpg?1783915101"

        ruling(
            "2023-09-01",
            "Roles are colorless enchantment tokens. Each one has the Aura and Role subtypes and the " +
                "enchant creature ability."
        )
        ruling(
            "2023-09-01",
            "If a permanent has more than one Role attached to it controlled by the same player, each " +
                "of those Roles except the one with the most recent timestamp is put into its owner's " +
                "graveyard. This is a state-based action."
        )
        ruling(
            "2023-09-01",
            "A permanent can have multiple Roles attached to it if each one is controlled by a " +
                "different player."
        )
    }
}
