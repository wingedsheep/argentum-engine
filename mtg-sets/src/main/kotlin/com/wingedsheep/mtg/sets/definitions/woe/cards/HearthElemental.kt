package com.wingedsheep.mtg.sets.definitions.woe.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.CostReductionSource
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget

/**
 * Hearth Elemental // Stoke Genius
 * {5}{R}
 * Creature — Elemental
 * 4/5
 * This spell costs {X} less to cast, where X is the number of cards in your graveyard that are
 * instant cards, sorcery cards, and/or have an Adventure.
 *
 * Adventure: Stoke Genius — {1}{R}, Sorcery — Adventure
 * Discard your hand, then draw two cards.
 *
 * The reduction is a self-cast [ModifySpellCost] over
 * [CostReductionSource.CardsInGraveyardMatchingFilter] with [Filters.InstantSorceryOrAdventure] —
 * the "and/or" is one membership test, not three counts, so a card that is both an instant and has
 * an Adventure is still counted once. It only ever eats generic mana, so the {R} always survives
 * and the mana value stays 6 no matter what was paid.
 *
 * Crucially the reduction does **not** apply to the Adventure half: per CR 715.3 a spell cast as an
 * Adventure has only the Adventure's characteristics, so the creature face's static ability isn't
 * there to reduce it. Stoke Genius always costs {1}{R}. The engine gets this right for free —
 * secondary faces price through `calculateEffectiveCostWithAlternativeBase`, which deliberately
 * skips `SelfCast` reductions.
 *
 * Stoke Genius is an ordered composite: discard the whole hand *first*, then draw two, so the drawn
 * cards are never discarded. An empty hand discards nothing and still draws two.
 *
 * (CR 715: Adventure cards. Casting the Adventure exiles the card on resolution and lets the caster
 * cast it as the creature spell while it remains in exile.)
 */
val HearthElemental = card("Hearth Elemental") {
    manaCost = "{5}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Elemental"
    oracleText = "This spell costs {X} less to cast, where X is the number of cards in your " +
        "graveyard that are instant cards, sorcery cards, and/or have an Adventure."
    power = 4
    toughness = 5

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.SelfCast,
            modification = CostModification.ReduceGenericBy(
                CostReductionSource.CardsInGraveyardMatchingFilter(
                    filter = Filters.InstantSorceryOrAdventure,
                    amountPerCard = 1,
                )
            ),
        )
    }

    adventure("Stoke Genius") {
        manaCost = "{1}{R}"
        typeLine = "Sorcery — Adventure"
        oracleText = "Discard your hand, then draw two cards. " +
            "(Then exile this card. You may cast the creature later from exile.)"
        spell {
            effect = Effects.Composite(
                Patterns.Hand.discardHand(),
                Effects.DrawCards(2),
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "136"
        artist = "Nicholas Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/a/8/a8f5f102-cc75-4cee-a117-4bdaaf86c2e9.jpg?1783915092"

        ruling(
            "2023-09-01",
            "When casting a spell as an Adventure, use the alternative characteristics and ignore " +
                "all of the card's normal characteristics. The spell's color, mana cost, mana " +
                "value, and so on are determined by only those alternative characteristics."
        )
        ruling(
            "2023-09-01",
            "An effect may refer to a card, spell, or permanent that \"has an Adventure.\" This " +
                "refers to a card, spell, or permanent that has an adventurer card's set of " +
                "alternative characteristics, even if they're not being used and even if that card " +
                "was never cast as an Adventure."
        )
        ruling(
            "2023-09-01",
            "If a spell is cast as an Adventure, its controller exiles it instead of putting it " +
                "into its owner's graveyard as it resolves. For as long as it remains exiled, that " +
                "player may cast it as a permanent spell."
        )
    }
}
