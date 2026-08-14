package com.wingedsheep.mtg.sets.definitions.dft.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.startYourEngines
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackUnless
import com.wingedsheep.sdk.scripting.CantBlockUnless

/**
 * Hazoret, Godseeker — Aetherdrift #133
 * {1}{R} · Legendary Creature — God · 5/3
 *
 * Indestructible, haste
 * Start your engines!
 * {1}, {T}: Target creature with power 2 or less can't be blocked this turn.
 * Hazoret can't attack or block unless you have max speed.
 *
 * The combat restriction is *not* a "Max speed — [ability]" line, so it is a plain pair of statics
 * rather than a [com.wingedsheep.sdk.dsl.maxSpeed] block: the printed ability exists at all speeds
 * and only its condition reads speed. Modelling it the other way round would be observably wrong —
 * `maxSpeed { }` also stamps the display-only [Keyword.MAX_SPEED] badge, and Scryfall lists only
 * "Start your engines!" for this card.
 *
 * `CantAttackUnless` / `CantBlockUnless` route their condition through the standard
 * `ConditionEvaluator`, so [Conditions.YouHaveMaxSpeed] (speed == 4) is re-read at each restriction
 * check — losing max speed mid-turn correctly re-imposes the restriction, and a creature that is
 * already attacking is unaffected (CR 506.4: restrictions are checked only as attackers/blockers are
 * declared).
 *
 * The activated ability is the Crafty Pathmage shape: a targeting restriction on power (the target
 * must have power 2 or less when the ability is activated *and* when it resolves, CR 608.2b), then a
 * flat [AbilityFlag.CANT_BE_BLOCKED] grant for the turn. Per the ruling, later power growth does not
 * strip the grant, which falls out of granting the flag rather than re-checking power in combat.
 */
val HazoretGodseeker = card("Hazoret, Godseeker") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Creature — God"
    oracleText = "Indestructible, haste\n" +
        "Start your engines! (If you have no speed, it starts at 1. It increases once on each of " +
        "your turns when an opponent loses life. Max speed is 4.)\n" +
        "{1}, {T}: Target creature with power 2 or less can't be blocked this turn.\n" +
        "Hazoret can't attack or block unless you have max speed."
    power = 5
    toughness = 3

    keywords(Keyword.INDESTRUCTIBLE, Keyword.HASTE)

    startYourEngines()

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap)
        val creature = target("creature", Targets.CreatureWithPowerAtMost(2))
        effect = Effects.GrantKeyword(AbilityFlag.CANT_BE_BLOCKED, creature)
        description = "Target creature with power 2 or less can't be blocked this turn."
    }

    staticAbility {
        ability = CantAttackUnless(Conditions.YouHaveMaxSpeed)
    }
    staticAbility {
        ability = CantBlockUnless(Conditions.YouHaveMaxSpeed)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "133"
        artist = "Chris Rallis"
        imageUri = "https://cards.scryfall.io/normal/front/e/2/e2f66043-0872-4334-a91f-0e9bbbdddf66.jpg?1783907881"
        ruling(
            "2025-02-07",
            "Hazoret's activated ability must be used prior to declaring blockers to be effective. " +
                "Activating it targeting a creature that has already been blocked will not cause that " +
                "creature to become unblocked."
        )
        ruling(
            "2025-02-07",
            "After the activated ability resolves, the creature can't be blocked this turn even if " +
                "its power later increases to 3 or greater."
        )
        ruling(
            "2025-02-07",
            "A player \"has max speed\" if their speed is 4."
        )
    }
}
