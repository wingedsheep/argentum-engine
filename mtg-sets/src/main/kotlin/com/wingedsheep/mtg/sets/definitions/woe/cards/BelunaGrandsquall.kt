package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.predicates.CardPredicate

/**
 * Beluna Grandsquall // Seek Thrills
 * {G}{U}{R}
 * Legendary Creature — Giant Noble
 * 4/4
 * Trample
 * Permanent spells you cast that have an Adventure cost {1} less to cast.
 *
 * Adventure: Seek Thrills — {2}{G}{U}{R}, Instant — Adventure
 * Mill seven cards. Then put all cards that have an Adventure from among the milled cards into
 * your hand.
 *
 * The discount is `YouCast(Permanent ∧ HasAdventure)` + [CostModification.ReduceGeneric] — "permanent
 * spells" is the load-bearing half of the wording. An adventurer card cast as its Adventure is an
 * instant or sorcery spell with only the Adventure's characteristics (CR 715.3b), so the discount must
 * not touch it.
 *
 * What actually keeps it off is the engine's *pricing path*, not the [Filters.Permanent] predicate:
 * cost filters are matched against the card definition (`CostCalculator.matchesCardDefinition`), i.e.
 * the front face, which is a permanent either way. Secondary faces price through
 * `calculateEffectiveCostWithAlternativeBase`, which consults only `AnyCaster` *increases* and never
 * `YouCast` reductions — an engine-wide simplification that CR 118.9d does not license in general.
 * `WoeCardsBatch12ScenarioTest` therefore pins the outcome ("Rip the Seams still costs {2}{W} with
 * Beluna out"), so closing that gap turns a silent rules break into a red test. The creature half cast
 * from hand — or cast from exile after its Adventure resolved — goes through the normal path with the
 * adventurer card's own type line, so it gets the {1}.
 *
 * Seek Thrills is [Patterns.Library.mill] (an `isMill = true` gather into the `"milled"` collection,
 * then a move to the graveyard) followed by a filtered [MoveCollectionEffect] back out of that same
 * collection. Filtering the collection rather than the graveyard is what makes "from among the milled
 * cards" mean these seven specifically, and there is no selection step because the oracle says "put
 * **all** cards that have an Adventure" — no choice, no ordering, no "you may".
 *
 * [Filters.HasAdventure] is `CardPredicate.HasAdventure`, a characteristic of the whole card in every
 * zone — per the WOE rulings it finds an adventurer card in the graveyard even though that card is
 * showing only its front-face (permanent) characteristics there.
 */
val BelunaGrandsquall = card("Beluna Grandsquall") {
    manaCost = "{G}{U}{R}"
    colorIdentity = "GUR"
    typeLine = "Legendary Creature — Giant Noble"
    oracleText = "Trample\n" +
        "Permanent spells you cast that have an Adventure cost {1} less to cast."
    power = 4
    toughness = 4

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(
                Filters.Permanent.withCardPredicate(CardPredicate.HasAdventure),
            ),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    adventure("Seek Thrills") {
        manaCost = "{2}{G}{U}{R}"
        typeLine = "Instant — Adventure"
        oracleText = "Mill seven cards. Then put all cards that have an Adventure from among the " +
            "milled cards into your hand. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            effect = Effects.Composite(
                Patterns.Library.mill(7).effects + MoveCollectionEffect(
                    from = "milled",
                    destination = CardDestination.ToZone(Zone.HAND),
                    filter = Filters.HasAdventure,
                ),
            )
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "220"
        artist = "Victor Adame Minguez"
        imageUri = "https://cards.scryfall.io/normal/front/3/f/3f5acc0d-33a6-476f-95ca-a1ad788334dd.jpg?1783915067"

        ruling(
            "2023-09-01",
            "An effect may refer to a card, spell, or permanent that \"has an Adventure.\" This " +
                "refers to a card, spell, or permanent that has an adventurer card's set of " +
                "alternative characteristics, even if they're not being used and even if that card " +
                "was never cast as an Adventure."
        )
        ruling(
            "2023-09-01",
            "When casting a spell as an Adventure, use the alternative characteristics and ignore " +
                "all of the card's normal characteristics. The spell's color, mana cost, mana " +
                "value, and so on are determined by only those alternative characteristics."
        )
        ruling(
            "2023-09-01",
            "An adventurer card is a permanent card in every zone except the stack, as well as " +
                "while on the stack if not cast as an Adventure. Ignore its alternative " +
                "characteristics in those cases."
        )
    }
}
