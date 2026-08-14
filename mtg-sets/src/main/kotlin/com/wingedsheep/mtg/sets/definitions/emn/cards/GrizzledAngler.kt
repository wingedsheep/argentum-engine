package com.wingedsheep.mtg.sets.definitions.emn.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.predicates.CardPredicate
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Grizzled Angler // Grisly Anglerfish (Eldritch Moon #63 — the card's earliest printing; also
 * reprinted in Innistrad Remastered)
 * {2}{U}
 * Creature — Human 2/3 // Creature — Eldrazi Fish 4/5
 *
 * Front — Grizzled Angler ({2}{U}, Creature — Human, 2/3)
 *   {T}: Mill two cards. Then if there is a colorless creature card in your graveyard,
 *   transform this creature.
 *
 * Back — Grisly Anglerfish (Creature — Eldrazi Fish, 4/5, colorless)
 *   {6}: Creatures your opponents control attack this turn if able.
 *
 * Implementation:
 *  - The front's tap ability is [Patterns.Library.mill] (2) followed by a [ConditionalEffect] on
 *    [Conditions.CardsInGraveyardMatchingAtLeast]`(1, colorless creature)` that flips the permanent
 *    with [TransformEffect] — the Treasure Map "do a thing, then conditionally transform" shape. The
 *    condition is re-read *after* the mill, so cards milled by this very activation count, and the
 *    flip only happens while the ability resolves (printed ruling: an already-present colorless
 *    creature card doesn't transform it on its own).
 *  - "Colorless creature card" is `GameObjectFilter.Creature` plus [CardPredicate.IsColorless] —
 *    a graveyard-zone read, so base characteristics are the right source (no projection needed).
 *  - The back's {6} ability marks every creature your opponents control with
 *    [Effects.MarkMustAttackThisTurn] via [Effects.ForEachInGroup] over
 *    [GroupFilter.AllCreaturesOpponentsControl] — `EffectTarget.Self` inside the ForEach body is
 *    the iterated creature. The marker is per-creature and this-turn only, and the engine's
 *    attack-requirement check already honours the printed rulings: a creature that can't attack
 *    (summoning sick, tapped, an attack restriction) doesn't, a cost to attack is never forced,
 *    and each controller still picks what their creature attacks.
 */

private val ColorlessCreatureCard: GameObjectFilter =
    GameObjectFilter.Creature.withCardPredicate(CardPredicate.IsColorless)

private val GrizzledAnglerFront = card("Grizzled Angler") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Human"
    power = 2
    toughness = 3
    oracleText = "{T}: Mill two cards. Then if there is a colorless creature card in your " +
        "graveyard, transform this creature. (To mill two cards, put the top two cards of your " +
        "library into your graveyard.)"

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.Composite(
            Patterns.Library.mill(2),
            ConditionalEffect(
                condition = Conditions.CardsInGraveyardMatchingAtLeast(1, ColorlessCreatureCard),
                effect = TransformEffect(EffectTarget.Self),
            ),
        )
        description = "Mill two cards. Then if there is a colorless creature card in your " +
            "graveyard, transform this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "63"
        artist = "Kev Walker"
        flavorText = "\"There's no question that these waters are treacherous, but if there's one " +
            "thing I've learned in all my years of sailing . . .\""
        imageUri = "https://cards.scryfall.io/normal/front/1/e/1eb4ddf4-f695-412d-be80-b93392432498.jpg?1783937503"
        ruling(
            "2025-01-24",
            "If you have a colorless creature card in your graveyard while you control Grizzled " +
                "Angler, it won't transform yet. It only transforms while its activated ability is " +
                "resolving."
        )
        ruling(
            "2025-01-24",
            "If a creature affected by Grisly Anglerfish's effect hasn't been under its " +
                "controller's control since the turn began, is tapped, or is affected by a spell or " +
                "ability that says it can't attack, then it doesn't attack. If there's a cost " +
                "associated with having that creature attack, its controller isn't forced to pay " +
                "that cost, so it doesn't have to attack in that case either."
        )
        ruling(
            "2025-01-24",
            "Each creature's controller still chooses the player, planeswalker, or battle it " +
                "attacks. The creatures that have to attack don't necessarily have to attack you if " +
                "there are other options."
        )
    }
}

private val GrislyAnglerfish = card("Grisly Anglerfish") {
    manaCost = ""
    colorIdentity = "U"
    typeLine = "Creature — Eldrazi Fish"
    power = 4
    toughness = 5
    oracleText = "{6}: Creatures your opponents control attack this turn if able."

    activatedAbility {
        cost = Costs.Mana("{6}")
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesOpponentsControl,
            effect = Effects.MarkMustAttackThisTurn(EffectTarget.Self),
        )
        description = "Creatures your opponents control attack this turn if able."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "63"
        artist = "Kev Walker"
        flavorText = "\". . . it's that the lure of the sea is impossible to ignore.\""
        imageUri = "https://cards.scryfall.io/normal/back/1/e/1eb4ddf4-f695-412d-be80-b93392432498.jpg?1783937503"
    }
}

val GrizzledAngler: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = GrizzledAnglerFront,
    backFace = GrislyAnglerfish,
)
