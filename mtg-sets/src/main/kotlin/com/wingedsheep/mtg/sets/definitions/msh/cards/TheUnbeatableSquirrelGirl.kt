package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player

/** MSH Squirrel token (Scryfall set `tmsh`, #14). */
private const val SQUIRREL_TOKEN_IMAGE =
    "https://cards.scryfall.io/normal/front/f/d/fd0474f3-682d-4c6d-b902-84f3250aa269.jpg?1783902800"

/**
 * The Unbeatable Squirrel Girl — Marvel Super Heroes #193 (rare)
 * {1}{G}{G}{G} · Legendary Creature — Squirrel Human Hero · 4/4
 *
 * Do You Like Squirrels? — Whenever The Unbeatable Squirrel Girl enters or attacks, create a 1/1
 * green Squirrel creature token.
 * I LOVE Squirrels! — {1}{G}{G}{G}: Create X 1/1 green Squirrel creature tokens, where X is the
 * number of Squirrels you control.
 *
 * Implementation notes:
 * - "Do You Like Squirrels?" and "I LOVE Squirrels!" are ability words (CR 207.2c) — flavor only,
 *   so they live in the oracle text and the ability [description]s, never as keywords.
 * - The "enters **or** attacks" idiom is two triggered abilities sharing one effect (the Threefold
 *   Thunderhulk / Queen's Bay Paladin split): the SDK has no single enters-or-attacks event, and
 *   authoring it as one would collapse the two independent firings a blink-then-attack turn
 *   produces.
 * - X on the activated ability is [DynamicAmounts.battlefield] `(You, Creature.withSubtype(SQUIRREL))
 *   .count()`, evaluated at resolution against projected state — so Squirrel Girl herself (a
 *   Squirrel) counts, as do tokens minted earlier in the turn and anything a type-changing effect
 *   has turned into a Squirrel. The ability can be activated with no Squirrels at all only in
 *   theory: Squirrel Girl is on the battlefield to activate it, so X is at least 1.
 * - Both token effects mint the MSH Squirrel token art (Scryfall set `tmsh`, #14).
 */
val TheUnbeatableSquirrelGirl = card("The Unbeatable Squirrel Girl") {
    manaCost = "{1}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Squirrel Human Hero"
    power = 4
    toughness = 4
    oracleText = "Do You Like Squirrels? — Whenever The Unbeatable Squirrel Girl enters or " +
        "attacks, create a 1/1 green Squirrel creature token.\n" +
        "I LOVE Squirrels! — {1}{G}{G}{G}: Create X 1/1 green Squirrel creature tokens, where X " +
        "is the number of Squirrels you control."

    // "Do You Like Squirrels? — Whenever ~ enters ..."
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf(Subtype.SQUIRREL.value),
            imageUri = SQUIRREL_TOKEN_IMAGE,
        )
        description = "Do You Like Squirrels? — Whenever The Unbeatable Squirrel Girl enters, " +
            "create a 1/1 green Squirrel creature token."
    }

    // "... or attacks, create a 1/1 green Squirrel creature token."
    triggeredAbility {
        trigger = Triggers.Attacks
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf(Subtype.SQUIRREL.value),
            imageUri = SQUIRREL_TOKEN_IMAGE,
        )
        description = "Do You Like Squirrels? — Whenever The Unbeatable Squirrel Girl attacks, " +
            "create a 1/1 green Squirrel creature token."
    }

    // "I LOVE Squirrels! — {1}{G}{G}{G}: Create X 1/1 green Squirrel creature tokens ..."
    activatedAbility {
        cost = Costs.Mana("{1}{G}{G}{G}")
        effect = Effects.CreateToken(
            count = DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature.withSubtype(Subtype.SQUIRREL),
            ).count(),
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf(Subtype.SQUIRREL.value),
            imageUri = SQUIRREL_TOKEN_IMAGE,
        )
        description = "I LOVE Squirrels! — Create X 1/1 green Squirrel creature tokens, where X " +
            "is the number of Squirrels you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "193"
        artist = "Kim Dingwall"
        flavorText = "\"We're here to eat nuts and kick butts!\""
        imageUri = "https://cards.scryfall.io/normal/front/d/8/d8b94ebb-0b3a-4e68-8c0c-3a61ac32f3ef.jpg?1783902911"
    }
}
