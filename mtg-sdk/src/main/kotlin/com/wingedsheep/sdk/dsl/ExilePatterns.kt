package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.GrantPlayWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Effect patterns for exile operations: exile-and-return, linked exile,
 * exile-as-replacement, and token replacement after removal.
 */
object ExilePatterns {

    fun exileUntilEndStep(target: EffectTarget): Effect = CompositeEffect(
        listOf(
            MoveToZoneEffect(target, Zone.EXILE),
            CreateDelayedTriggerEffect(
                step = Step.END,
                effect = MoveToZoneEffect(target, Zone.BATTLEFIELD)
            )
        )
    )

    fun exileGroupAndLink(
        filter: GroupFilter,
        storeAs: String = "linked_exile"
    ): CompositeEffect = CompositeEffect(listOf(
        GatherCardsEffect(
            source = CardSource.BattlefieldMatching(
                filter = filter.baseFilter,
                excludeSelf = true
            ),
            storeAs = storeAs
        ),
        MoveCollectionEffect(
            from = storeAs,
            destination = CardDestination.ToZone(Zone.EXILE),
            linkToSource = true
        )
    ))

    fun returnLinkedExile(
        underOwnersControl: Boolean = false,
        storeAs: String = "linked_return"
    ): CompositeEffect = CompositeEffect(listOf(
        GatherCardsEffect(
            source = CardSource.FromLinkedExile(),
            storeAs = storeAs
        ),
        MoveCollectionEffect(
            from = storeAs,
            destination = CardDestination.ToZone(Zone.BATTLEFIELD),
            underOwnersControl = underOwnersControl
        )
    ))

    fun returnLinkedExileToHand(storeAs: String = "linked_return_hand"): CompositeEffect = CompositeEffect(listOf(
        GatherCardsEffect(
            source = CardSource.FromLinkedExile(),
            storeAs = storeAs
        ),
        MoveCollectionEffect(
            from = storeAs,
            destination = CardDestination.ToZone(Zone.HAND)
        )
    ))

    fun takeFromLinkedExile(storeAs: String = "linked_take"): CompositeEffect = CompositeEffect(listOf(
        GatherCardsEffect(
            source = CardSource.FromLinkedExile(count = 1),
            storeAs = storeAs
        ),
        MoveCollectionEffect(
            from = storeAs,
            destination = CardDestination.ToZone(Zone.HAND),
            unlinkFromSource = true
        )
    ))

    /**
     * Impulse draw: exile the top [count] cards of your library, then grant permission to play
     * those cards until [expiry] (default end of turn). The played cards still cost their mana;
     * for the "play it without paying its mana cost" variant compose with
     * [GrantPlayWithoutPayingCostEffect] (see [shuffleAndExileTopPlayFree]).
     *
     * Named MTG mechanic ("impulse draw") with multiple users — Irascible Wolverine (1),
     * Annie Flash, the Veteran (2), etc.
     */
    fun impulse(
        count: Int = 1,
        expiry: MayPlayExpiry = MayPlayExpiry.EndOfTurn,
        storeAs: String = "impulseExiled"
    ): Effect = CompositeEffect(listOf(
        GatherCardsEffect(
            source = CardSource.TopOfLibrary(DynamicAmount.Fixed(count)),
            storeAs = storeAs
        ),
        MoveCollectionEffect(
            from = storeAs,
            destination = CardDestination.ToZone(Zone.EXILE)
        ),
        GrantMayPlayFromExileEffect(storeAs, expiry)
    ))

    fun shuffleAndExileTopPlayFree(): Effect = CompositeEffect(listOf(
        ShuffleLibraryEffect(),
        GatherCardsEffect(
            source = CardSource.TopOfLibrary(DynamicAmount.Fixed(1)),
            storeAs = "exiledCard"
        ),
        MoveCollectionEffect(
            from = "exiledCard",
            destination = CardDestination.ToZone(Zone.EXILE)
        ),
        GrantMayPlayFromExileEffect("exiledCard"),
        GrantPlayWithoutPayingCostEffect("exiledCard")
    ))

    fun exileAndReplaceWithToken(
        target: EffectTarget,
        power: Int,
        toughness: Int,
        colors: Set<Color> = emptySet(),
        creatureTypes: Set<String>,
        keywords: Set<Keyword> = emptySet()
    ): CompositeEffect = CompositeEffect(
        listOf(
            MoveToZoneEffect(target, Zone.EXILE),
            CreateTokenEffect(
                power = power,
                toughness = toughness,
                colors = colors,
                creatureTypes = creatureTypes,
                keywords = keywords,
                controller = EffectTarget.TargetController
            )
        )
    )

    fun destroyAndReplaceWithToken(
        target: EffectTarget,
        power: Int,
        toughness: Int,
        colors: Set<Color> = emptySet(),
        creatureTypes: Set<String>,
        keywords: Set<Keyword> = emptySet()
    ): CompositeEffect = CompositeEffect(
        listOf(
            MoveToZoneEffect(target, Zone.GRAVEYARD, byDestruction = true),
            CreateTokenEffect(
                power = power,
                toughness = toughness,
                colors = colors,
                creatureTypes = creatureTypes,
                keywords = keywords,
                controller = EffectTarget.TargetController
            )
        )
    )
}
