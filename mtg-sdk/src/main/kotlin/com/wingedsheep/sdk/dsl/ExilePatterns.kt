package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.CreateTokenEffect
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GrantMayPlayFromExileEffect
import com.wingedsheep.sdk.scripting.effects.GrantPlayWithoutPayingCostEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.MoveToZoneEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.ShuffleLibraryEffect
import com.wingedsheep.sdk.scripting.effects.StoreResultEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount
import com.wingedsheep.sdk.scripting.values.EffectVariable

/**
 * Effect patterns for exile operations: exile-and-return, linked exile,
 * exile-as-replacement, and token replacement after removal.
 */
object ExilePatterns {

    fun exileUntilLeaves(
        exileTarget: EffectTarget,
        variableName: String = "exiledCard"
    ): StoreResultEffect = StoreResultEffect(
        effect = MoveToZoneEffect(exileTarget, Zone.EXILE),
        storeAs = EffectVariable.EntityRef(variableName)
    )

    fun exileUntilEndStep(target: EffectTarget): Effect = CompositeEffect(
        listOf(
            MoveToZoneEffect(target, Zone.EXILE),
            CreateDelayedTriggerEffect(
                step = Step.END,
                effect = MoveToZoneEffect(target, Zone.BATTLEFIELD)
            )
        )
    )

    fun searchAndExileLinked(
        count: Int = 7,
        filter: GameObjectFilter = GameObjectFilter.Any
    ): CompositeEffect = CompositeEffect(listOf(
        GatherCardsEffect(
            source = CardSource.FromZone(Zone.LIBRARY, Player.You, filter),
            storeAs = "searchable"
        ),
        SelectFromCollectionEffect(
            from = "searchable",
            selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(count)),
            storeSelected = "found"
        ),
        MoveCollectionEffect(
            from = "found",
            destination = CardDestination.ToZone(Zone.EXILE),
            order = CardOrder.Random,
            linkToSource = true,
            faceDown = true
        ),
        ShuffleLibraryEffect()
    ))

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

    fun eachPlayerRevealCreaturesCreateTokens(
        tokenPower: Int,
        tokenToughness: Int,
        tokenColors: Set<Color>,
        tokenCreatureTypes: Set<String>,
        tokenImageUri: String? = null
    ): ForEachPlayerEffect = ForEachPlayerEffect(
        players = Player.Each,
        effects = listOf(
            GatherCardsEffect(
                source = CardSource.FromZone(Zone.HAND, Player.You, GameObjectFilter.Creature),
                storeAs = "creatures"
            ),
            SelectFromCollectionEffect(
                from = "creatures",
                selection = SelectionMode.ChooseAnyNumber,
                storeSelected = "revealed",
                prompt = "You may reveal any number of creature cards from your hand"
            ),
            CreateTokenEffect(
                count = DynamicAmount.VariableReference("revealed_count"),
                power = tokenPower,
                toughness = tokenToughness,
                colors = tokenColors,
                creatureTypes = tokenCreatureTypes,
                imageUri = tokenImageUri
            )
        )
    )
}
