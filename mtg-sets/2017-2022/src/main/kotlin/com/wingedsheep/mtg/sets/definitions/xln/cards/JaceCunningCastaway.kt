package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.grantedTriggeredAbility
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggeredAbility
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jace, Cunning Castaway
 * {1}{U}{U}
 * Legendary Planeswalker — Jace
 * Starting Loyalty: 3
 *
 * +1: Whenever one or more creatures you control deal combat damage to a player this turn, draw a
 * card, then discard a card.
 * −2: Create a 2/2 blue Illusion creature token with "When this token becomes the target of a
 * spell, sacrifice it."
 * −5: Create two tokens that are copies of Jace, except they're not legendary.
 *
 * - The +1 creates a floating "this turn, whenever …" ability (CR 603.7) owned by Jace's controller,
 *   not by Jace: [Effects.CreateGlobalTriggeredAbility] for [Duration.EndOfTurn] (the Mistway Spy
 *   shape), so it keeps working if Jace leaves. Its trigger is the batched
 *   `Triggers.oneOrMore(Creature).dealCombatDamageToAPlayer()` — global abilities join the
 *   combat-damage batch detector — so it fires once per player hit per combat-damage step, and again
 *   for a separate first-strike step (ruling).
 * - The −2 token carries the Departed Deckhand trigger (`becomesTarget(spellsOnly = true)` →
 *   sacrifice itself).
 * - The −5 copies Jace's copiable values, falling back to last-known information when paying −5
 *   already sent Jace to the graveyard; each copy enters with its printed three loyalty.
 */
val JaceCunningCastaway = card("Jace, Cunning Castaway") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Planeswalker — Jace"
    startingLoyalty = 3
    oracleText = "+1: Whenever one or more creatures you control deal combat damage to a player this turn, " +
        "draw a card, then discard a card.\n" +
        "−2: Create a 2/2 blue Illusion creature token with \"When this token becomes the target of a " +
        "spell, sacrifice it.\"\n" +
        "−5: Create two tokens that are copies of Jace, except they're not legendary."

    // +1: Whenever one or more creatures you control deal combat damage to a player this turn,
    // draw a card, then discard a card.
    loyaltyAbility(+1) {
        effect = Effects.CreateGlobalTriggeredAbility(
            duration = Duration.EndOfTurn,
            ability = TriggeredAbility.create(
                trigger = Triggers.oneOrMore(GameObjectFilter.Creature).dealCombatDamageToAPlayer(),
                effect = Patterns.Hand.loot(),
            ),
            descriptionOverride = "Whenever one or more creatures you control deal combat damage to a " +
                "player this turn, draw a card, then discard a card.",
        )
    }

    // −2: Create a 2/2 blue Illusion creature token with "When this token becomes the target of a
    // spell, sacrifice it."
    loyaltyAbility(-2) {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.BLUE),
            creatureTypes = setOf("Illusion"),
            triggeredAbilities = listOf(
                grantedTriggeredAbility {
                    trigger = Triggers.self.becomesTarget(spellsOnly = true)
                    effect = Effects.SacrificeTarget(EffectTarget.Self)
                    description = "When this token becomes the target of a spell, sacrifice it."
                }
            ),
            imageUri = "https://cards.scryfall.io/normal/front/a/1/a10729a5-061a-4daf-91d6-0f6ce813a992.jpg?1783935810",
        )
    }

    // −5: Create two tokens that are copies of Jace, except they're not legendary.
    loyaltyAbility(-5) {
        effect = Effects.CreateTokenCopyOfSelf(count = 2, removeLegendary = true)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "60"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/b/7/b75f56f2-88fd-44ba-ad02-56ea3391f173.jpg?1783935780"
        ruling(
            "2017-09-29",
            "The delayed triggered ability created by Jace's first ability can trigger more than once in a turn if creatures you control deal combat damage at different times in a turn (most likely because one or more has first strike) or if creatures you control deal combat damage to more than one player at once.",
        )
        ruling(
            "2017-09-29",
            "If a token created by Jace's second ability becomes the target of a spell, its ability triggers and goes on the stack on top of that spell. The ability will resolve first (causing the token to be sacrificed). Unless the spell has another target, it will then be countered when it tries to resolve for having no legal targets.",
        )
        ruling(
            "2017-09-29",
            "The tokens from Jace's last ability copy exactly what is printed on Jace and nothing else. They don't copy how many counters are on him or Auras attached to him, or any non-copy effects that have changed his color or types, and so on. They'll each enter the battlefield with three loyalty counters. You can activate one loyalty ability of each of the tokens this turn.",
        )
        ruling(
            "2017-09-29",
            "The tokens created by Jace's last ability don't have the legendary supertype. If another object becomes a copy of the token, that copy also won't be legendary.",
        )
    }
}
