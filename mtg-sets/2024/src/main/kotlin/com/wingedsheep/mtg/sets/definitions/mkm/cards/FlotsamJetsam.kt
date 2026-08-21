package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardLayout
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Flotsam // Jetsam — Murders at Karlov Manor #247
 * {1}{G/U} // {4}{U/B}{U/B} · Instant // Sorcery
 *
 * Flotsam — Mill three cards. Investigate.
 * Jetsam — Each opponent mills three cards, then you may cast a spell from each opponent's
 * graveyard without paying its mana cost. If a spell cast this way would be put into a graveyard,
 * exile it instead.
 *
 * Cast either half, never both (CR 709.4).
 *
 * Flotsam is two existing keyword actions back to back and needs no comment. Jetsam is the
 * interesting half, and its shape is dictated by one word: **each**. "A spell from each opponent's
 * graveyard" is a per-opponent question, not one selection over a merged pool, so it is a
 * `ForEachPlayer(EachOpponent)` iteration whose body gathers *that* opponent's graveyard —
 * `Player.You` inside the loop is the iterated player.
 *
 * That rebinding is also the trap. The iteration deliberately rebinds the context's controller so
 * `Player.You` names the opponent, but *you* are still the one choosing and the one casting, so
 * both the selection and the cast take [Chooser.SourceController] — the chooser that reads through
 * a per-iteration swap to the spell's own controller. Without it Jetsam would hand each opponent
 * their own graveyard and let them cast out of it.
 *
 * `ChooseUpTo(1)` per opponent is the "may" *and* the "a spell": at most one card leaves each
 * graveyard, and declining for one opponent doesn't forfeit the others. The filter is `Nonland`
 * because a land isn't a spell; every other timing restriction is ignored anyway, since per the
 * 2024-02-02 ruling the casts happen during Jetsam's own resolution.
 *
 * `insteadOfGraveyard = EXILE` is the printed rider, stamped on each card as it is cast. It is
 * what stops an opponent's graveyard from simply refilling with the spell you just took, and it
 * applies whether the spell resolves, fizzles or is countered.
 */
val FlotsamJetsam = card("Flotsam // Jetsam") {
    layout = CardLayout.SPLIT
    colorIdentity = "GUB"

    face("Flotsam") {
        manaCost = "{1}{G/U}"
        typeLine = "Instant"
        oracleText = "Mill three cards. Investigate. (Create a Clue token. It's an artifact with " +
            "\"{2}, Sacrifice this token: Draw a card.\")"

        spell {
            effect = Effects.Composite(
                Patterns.Library.mill(3),
                Effects.Investigate()
            )
        }
    }

    face("Jetsam") {
        manaCost = "{4}{U/B}{U/B}"
        typeLine = "Sorcery"
        oracleText = "Each opponent mills three cards, then you may cast a spell from each " +
            "opponent's graveyard without paying its mana cost. If a spell cast this way would " +
            "be put into a graveyard, exile it instead."

        spell {
            effect = Effects.Composite(
                Patterns.Library.mill(3, EffectTarget.PlayerRef(Player.EachOpponent)),
                Effects.ForEachPlayer(
                    Player.EachOpponent,
                    listOf(
                        GatherCardsEffect(
                            source = CardSource.FromZone(
                                zone = Zone.GRAVEYARD,
                                player = Player.You,
                                filter = GameObjectFilter.Nonland
                            ),
                            storeAs = "theirGraveyard"
                        ),
                        SelectFromCollectionEffect(
                            from = "theirGraveyard",
                            selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                            chooser = Chooser.SourceController,
                            storeSelected = "toCast",
                            // No `showAllCards`: an opponent's graveyard is public, and a
                            // land-only graveyard would otherwise raise a picker with nothing
                            // selectable in it.
                            prompt = "Cast a spell from that opponent's graveyard?"
                        ),
                        Effects.CastFromCollectionWithoutPayingCost(
                            from = "toCast",
                            insteadOfGraveyard = AfterResolveDestination.EXILE,
                            caster = Chooser.SourceController
                        )
                    )
                )
            )
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "247"
        artist = "Anastasia Ovchinnikova"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c1500cbf-5619-465e-a97b-75e676ce789b.jpg?1783912830"

        ruling(
            "2024-02-02",
            "You choose whether to cast spells from opponents' graveyards as Jetsam resolves. If " +
                "you do, you do so as part of the resolution of Jetsam. You can't wait to cast " +
                "them later in the turn. Timing restrictions based on the cards' types are ignored."
        )
        ruling(
            "2024-02-02",
            "If you cast a spell \"without paying its mana cost\", you can't choose to cast it " +
                "for any alternative costs. You can, however, pay additional costs, such as " +
                "kicker costs. If the card has any mandatory additional costs, those must be " +
                "paid to cast the spell."
        )
        ruling(
            "2024-02-02",
            "If a spell you cast has {X} in its mana cost, you must choose 0 as the value of X " +
                "when casting it without paying its mana cost."
        )
        ruling(
            "2024-02-02",
            "To cast a split card, choose one of its halves to cast. There's no way to cast both " +
                "halves of any of the split cards featured in this set."
        )
    }
}
