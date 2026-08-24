package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.OnEnterRunEffect
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveType
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Frankenstein's Monster
 * {X}{B}{B}
 * Creature — Zombie
 * 0/1
 * As this creature enters, exile X creature cards from your graveyard. If you can't, put this
 * creature into its owner's graveyard instead of onto the battlefield. For each creature card
 * exiled this way, this creature enters with a +2/+0, +1/+1, or +0/+2 counter on it.
 *
 * The first sentence is all-or-nothing and the gate has to be asked before anything moves: a
 * `ConditionalEffect` on "creature cards in your graveyard >= X" runs the exile-and-count branch,
 * and its else branch is the failed entry. Left ungated, `SelectionMode.ChooseExactly` clamps to
 * the number of eligible cards, so an X the graveyard can't pay — whether overpaid on purpose or
 * emptied in response to the spell — would quietly build a smaller Monster instead of none.
 *
 * The counters are the interesting half. "For each card exiled, choose one of three" is X
 * independent choices among the same three options, all landing on this creature — which is
 * exactly a repeatable modal: `ModalEffect` with `allowRepeat` and both the dynamic ceiling and
 * the dynamic floor pinned to X, so the player makes exactly X choices and may make the same one
 * every time. Iterating the exiled pile instead would fight the pipeline, since inside a
 * collection iteration `EffectTarget.Self` means the *iterated card*, not the Monster.
 *
 * +2/+0 and +0/+2 are new counter kinds. They are genuinely distinct from two +1/+0 counters
 * (CR 122.1a), so they are their own enum members with their own P/T arithmetic rather than being
 * approximated by doubling an existing kind.
 *
 * **Known divergence.** "…instead of onto the battlefield" is a true entry replacement, but
 * `OnEnterRunEffect` runs just *after* the permanent is on the battlefield, so the Monster is
 * modelled as entering and then being put into its owner's graveyard when the graveyard can't pay.
 * The observable difference is that the entry has already happened: an "enters the battlefield"
 * trigger elsewhere sees it, and so does a "whenever a creature dies" one. Closing that needs a
 * pre-entry replacement hook, which is engine work rather than card work.
 */
val FrankensteinsMonster = card("Frankenstein's Monster") {
    manaCost = "{X}{B}{B}"
    typeLine = "Creature — Zombie"
    power = 0
    toughness = 1
    oracleText = "As this creature enters, exile X creature cards from your graveyard. If you " +
        "can't, put this creature into its owner's graveyard instead of onto the battlefield. " +
        "For each creature card exiled this way, this creature enters with a +2/+0, +1/+1, or " +
        "+0/+2 counter on it."

    replacementEffect(
        OnEnterRunEffect(
            // "If you can't" is the first sentence's teeth: the exile is all-or-nothing on X, so the
            // gate is asked *before* anything is exiled and the else branch is the entry failing.
            // Without it `ChooseExactly` quietly clamps to however many creature cards the graveyard
            // happens to hold, and an overpaid X — or a graveyard emptied in response to the spell —
            // yields a smaller Monster instead of no Monster.
            ConditionalEffect(
                condition = Conditions.CompareAmounts(
                    DynamicAmount.Count(Player.You, Zone.GRAVEYARD, GameObjectFilter.Creature),
                    ComparisonOperator.GTE,
                    DynamicAmount.XValue,
                ),
                effect = Effects.Composite(
                    GatherCardsEffect(
                        source = CardSource.FromZone(
                            zone = Zone.GRAVEYARD,
                            player = Player.You,
                            filter = GameObjectFilter.Creature,
                        ),
                        storeAs = "fmCandidates",
                    ),
                    SelectFromCollectionEffect(
                        from = "fmCandidates",
                        selection = SelectionMode.ChooseExactly(DynamicAmount.XValue),
                        storeSelected = "fmExiled",
                    ),
                    MoveCollectionEffect(
                        from = "fmExiled",
                        destination = CardDestination.ToZone(Zone.EXILE),
                    ),
                    ModalEffect(
                        modes = listOf(
                            Mode(
                                description = "Put a +2/+0 counter on this creature",
                                effect = Effects.AddCounters(Counters.PLUS_TWO_PLUS_ZERO, 1, EffectTarget.Self),
                            ),
                            Mode(
                                description = "Put a +1/+1 counter on this creature",
                                effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
                            ),
                            Mode(
                                description = "Put a +0/+2 counter on this creature",
                                effect = Effects.AddCounters(Counters.PLUS_ZERO_PLUS_TWO, 1, EffectTarget.Self),
                            ),
                        ),
                        allowRepeat = true,
                        countsAsModalSpell = false,
                        dynamicChooseCount = DynamicAmount.XValue,
                        dynamicMinChooseCount = DynamicAmount.XValue,
                    ),
                ),
                // Not a sacrifice and not a destruction: the card says "put into its owner's
                // graveyard", so nothing here should be stoppable by indestructible or by a
                // "can't be sacrificed" clause.
                elseEffect = Effects.Move(EffectTarget.Self, Zone.GRAVEYARD),
            )
        )
    )

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "45"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/8/f/8f99894d-5ece-44f1-acce-474494ae2084.jpg?1783947939"
    }
}
