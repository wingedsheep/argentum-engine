package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.AddCountersToCollectionEffect
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.EmitLibrarySearchedEventEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Vision Quest — Marvel Super Heroes #237 (rare)
 * {X}{U}{R} · Sorcery
 *
 * Search your library and/or graveyard for an artifact creature card with mana value X or less and
 * put it onto the battlefield with X additional +1/+1 counters on it. If X is 4 or greater, it
 * gains haste until end of turn. If you search your library this way, shuffle.
 *
 * A hand-rolled [com.wingedsheep.sdk.dsl.LibraryPatterns.searchMultipleZones]: the packaged pattern
 * takes a fixed `count` and no `storeMovedAs`, and this card needs both an X-driven *filter* and a
 * handle on the creature that arrived, so the same four pipeline steps are spelled out here.
 *
 *  1. [GatherCardsEffect] over [CardSource.FromMultipleZones] (`LIBRARY` + `GRAVEYARD`) — "your
 *     library and/or graveyard" is one combined pool, so a single selection covers both zones. The
 *     mana-value cap is `manaValueAtMostDynamic(DynamicAmount.XValue)`; `PredicateContext` carries
 *     the chosen X into predicate evaluation, so the pool is filtered against the X actually paid.
 *  2. [SelectFromCollectionEffect] with `ChooseUpTo(1)` — searching never *requires* finding
 *     (CR 701.23b), so failing to find is legal and the rest of the pipeline no-ops.
 *  3. [MoveCollectionEffect] to the battlefield, recapturing the arrival as `visionQuestCreature`.
 *  4. [AddCountersToCollectionEffect] with a resolution-time `amount` of X drops the +1/+1
 *     counters on it. (The counters are placed immediately after the creature arrives rather than
 *     literally *as* it enters — the collection-move primitive can only stamp a single counter of
 *     one kind on entry, so a dynamic count has to follow the move.)
 *
 * "If X is 4 or greater" is an ordinary resolution-time [Conditions.CompareAmounts] over the same
 * `XValue`, gating a [Duration.EndOfTurn] haste grant on [EffectTarget.PipelineTarget] — with
 * nothing found, the pipeline handle is empty and the grant is a no-op. The trailing
 * [ShuffleLibraryEffect] / [EmitLibrarySearchedEventEffect] pair mirrors what every other
 * library search emits (CR 701.23 triggers fire whether or not a card was found).
 */
val VisionQuest = card("Vision Quest") {
    manaCost = "{X}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Sorcery"
    oracleText = "Search your library and/or graveyard for an artifact creature card with mana " +
        "value X or less and put it onto the battlefield with X additional +1/+1 counters on it. " +
        "If X is 4 or greater, it gains haste until end of turn. If you search your library this " +
        "way, shuffle."

    spell {
        effect = Effects.Composite(
            listOf(
                GatherCardsEffect(
                    source = CardSource.FromMultipleZones(
                        zones = listOf(Zone.LIBRARY, Zone.GRAVEYARD),
                        player = Player.You,
                        filter = GameObjectFilter.ArtifactCreature
                            .manaValueAtMostDynamic(DynamicAmount.XValue)
                    ),
                    storeAs = "visionQuestSearchable"
                ),
                SelectFromCollectionEffect(
                    from = "visionQuestSearchable",
                    selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
                    storeSelected = "visionQuestFound",
                    prompt = "Search your library and/or graveyard for an artifact creature card " +
                        "with mana value X or less."
                ),
                MoveCollectionEffect(
                    from = "visionQuestFound",
                    destination = CardDestination.ToZone(Zone.BATTLEFIELD),
                    storeMovedAs = "visionQuestCreature"
                ),
                AddCountersToCollectionEffect(
                    collectionName = "visionQuestCreature",
                    counterType = Counters.PLUS_ONE_PLUS_ONE,
                    amount = DynamicAmount.XValue
                ),
                ConditionalEffect(
                    condition = Conditions.CompareAmounts(
                        DynamicAmount.XValue,
                        ComparisonOperator.GTE,
                        DynamicAmount.Fixed(4)
                    ),
                    effect = Effects.GrantKeyword(
                        Keyword.HASTE,
                        EffectTarget.PipelineTarget("visionQuestCreature"),
                        Duration.EndOfTurn
                    )
                ),
                ShuffleLibraryEffect(),
                EmitLibrarySearchedEventEffect
            )
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "237"
        artist = "Eglė Mosakaitė"
        flavorText = "\"Is there a problem, Wanda?\""
        imageUri = "https://cards.scryfall.io/normal/front/c/0/c01afea6-645d-4d4f-bdaa-90794a628bcd.jpg?1783902895"
    }
}
