package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetCreature
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Heroes' Hangout
 * {R}
 * Sorcery
 *
 * Choose one —
 * • Date Night — Exile the top two cards of your library. Choose one of them. Until the end of
 *   your next turn, you may play that card.
 * • Patrol Night — One or two target creatures each get +1/+0 and gain first strike until end of
 *   turn.
 *
 * Modelling notes:
 * - Modal "choose one" via [ModalEffect.chooseOne].
 * - Date Night is a *selective* impulse: both top cards are exiled, but only the chosen one becomes
 *   playable. Composed from the standard reveal-and-choose pipeline — [GatherCardsEffect] over
 *   [CardSource.TopOfLibrary] (two) → [SelectFromCollectionEffect] `ChooseExactly(1)` with
 *   `showAllCards` (see both, pick one) → [MoveCollectionEffect] sending *all* gathered cards to
 *   exile → [GrantMayPlayFromExileEffect] over only the `chosen` subset with
 *   [MayPlayExpiry.UntilEndOfNextTurn] ("until the end of your next turn"). This differs from the
 *   plain [com.wingedsheep.sdk.dsl.Patterns.Exile.impulse], which grants may-play on *every* exiled
 *   card; here the non-chosen card stays exiled and unplayable.
 * - Patrol Night is a one-or-two-target group buff: [TargetCreature] `count = 2, minCount = 1` fanned
 *   out with [ForEachTargetEffect] so each targeted creature independently gets +1/+0
 *   ([Effects.ModifyStats]) and gains first strike ([Effects.GrantKeyword]) until end of turn — the
 *   same one-or-two-target shape as Amazing Acrobatics.
 */
val HeroesHangout = card("Heroes' Hangout") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Date Night — Exile the top two cards of your library. Choose one of them. Until the end of your next turn, you may play that card.\n" +
        "• Patrol Night — One or two target creatures each get +1/+0 and gain first strike until end of turn."

    spell {
        effect = ModalEffect.chooseOne(
            Mode.noTarget(
                effect = Effects.Composite(
                    listOf(
                        GatherCardsEffect(
                            source = CardSource.TopOfLibrary(DynamicAmount.Fixed(2)),
                            storeAs = "exiledCards",
                        ),
                        SelectFromCollectionEffect(
                            from = "exiledCards",
                            selection = SelectionMode.ChooseExactly(DynamicAmount.Fixed(1)),
                            storeSelected = "chosen",
                            showAllCards = true,
                            prompt = "Choose one of the exiled cards to play.",
                        ),
                        MoveCollectionEffect(
                            from = "exiledCards",
                            destination = CardDestination.ToZone(Zone.EXILE),
                        ),
                        GrantMayPlayFromExileEffect(
                            from = "chosen",
                            expiry = MayPlayExpiry.UntilEndOfNextTurn,
                        ),
                    )
                ),
                description = "Date Night — Exile the top two cards of your library. Choose one of them. Until the end of your next turn, you may play that card.",
            ),
            Mode.withTarget(
                effect = ForEachTargetEffect(
                    listOf(
                        Effects.ModifyStats(1, 0, EffectTarget.ContextTarget(0), Duration.EndOfTurn),
                        Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.ContextTarget(0), Duration.EndOfTurn),
                    )
                ),
                target = TargetCreature(count = 2, minCount = 1),
                description = "Patrol Night — One or two target creatures each get +1/+0 and gain first strike until end of turn.",
            ),
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "79"
        artist = "Smirtouille"
        imageUri = "https://cards.scryfall.io/normal/front/4/1/4148d7e8-6371-468c-858b-35254995409a.jpg?1783905337"
    }
}
