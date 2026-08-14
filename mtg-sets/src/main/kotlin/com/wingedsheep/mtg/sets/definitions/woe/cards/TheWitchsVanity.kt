package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetCreature

/**
 * The Witch's Vanity
 * {1}{B}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Destroy target creature an opponent controls with mana value 2 or less.
 * II — Create a Food token.
 * III — Create a Wicked Role token attached to target creature you control.
 *
 * All three chapters sit on existing primitives. Chapter I is a plain destroy over a
 * `Creature.opponentControls().manaValueAtMost(2)` target — mana value is read from the card, so a
 * token (mana value 0) or an animated land is a legal target, while a creature pumped past 2 mana
 * value by a cost-changing effect is not.
 *
 * Chapter III mints the shared Wicked Role Aura token
 * ([com.wingedsheep.mtg.sets.tokens.PredefinedTokens.WickedRole]) attached to a creature you
 * control. Per the Role rulings, if that creature already carries another Role you control, the
 * older one is put into its owner's graveyard as a state-based action — which is itself what
 * triggers Wicked Visitor and friends. Both targeting chapters fizzle entirely if their target is
 * gone by resolution, so no Role token is created.
 */
val TheWitchsVanity = card("The Witch's Vanity") {
    manaCost = "{1}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Destroy target creature an opponent controls with mana value 2 or less.\n" +
        "II — Create a Food token. (It's an artifact with \"{2}, {T}, Sacrifice this token: You gain 3 life.\")\n" +
        "III — Create a Wicked Role token attached to target creature you control."

    sagaChapter(1) {
        val creature = target(
            "target creature an opponent controls with mana value 2 or less",
            TargetCreature(
                filter = TargetFilter(GameObjectFilter.Creature.opponentControls().manaValueAtMost(2)),
            ),
        )
        effect = Effects.Destroy(creature)
    }

    sagaChapter(2) {
        effect = Effects.CreateFood()
    }

    sagaChapter(3) {
        val creature = target("target creature you control", Targets.CreatureYouControl)
        effect = Effects.CreateRoleToken("Wicked Role", creature)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "119"
        artist = "Alix Branwyn"
        imageUri = "https://cards.scryfall.io/normal/front/4/7/47ca4926-b5ac-405a-8b58-f8db6df400ff.jpg?1783915098"

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
            "Some spells and abilities that create Role tokens require targets. If each target chosen " +
                "is an illegal target as that spell or ability tries to resolve, it won't resolve. The " +
                "Role token won't be created."
        )
        ruling(
            "2024-11-08",
            "Food is an artifact type. Even though it appears on some creatures, it's never a creature type."
        )
    }
}
