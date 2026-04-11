package com.wingedsheep.sdk.dsl

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ChooseActionEffect
import com.wingedsheep.sdk.scripting.effects.CompositeEffect
import com.wingedsheep.sdk.scripting.effects.GatherCardsEffect
import com.wingedsheep.sdk.scripting.effects.GatherSubtypesEffect
import com.wingedsheep.sdk.scripting.effects.MoveCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectFromCollectionEffect
import com.wingedsheep.sdk.scripting.effects.SelectionMode
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.ForEachInGroupEffect
import com.wingedsheep.sdk.scripting.effects.ForEachPlayerEffect
import com.wingedsheep.sdk.scripting.effects.ForEachTargetEffect
import com.wingedsheep.sdk.scripting.effects.OptionalCostEffect
import com.wingedsheep.sdk.scripting.effects.ReflexiveTriggerEffect
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.effects.StoreCountEffect
import com.wingedsheep.sdk.scripting.effects.StoreResultEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Facade for creating common effect patterns.
 *
 * Delegates to domain-specific pattern objects:
 * - [LibraryPatterns] — library manipulation (search, scry, surveil, mill, reveal-until)
 * - [HandPatterns] — hand manipulation (discard, draw, loot, wheel)
 * - [CreatureTypePatterns] — creature type choice effects
 * - [GroupPatterns] — bulk operations on filtered groups
 * - [ExilePatterns] — exile-and-return, linked exile, token replacement
 * - [MiscPatterns] — optional costs, sacrifice, reflexive triggers, utilities
 */
object EffectPatterns {

    // =========================================================================
    // Optional Cost Patterns (MiscPatterns)
    // =========================================================================

    fun mayPay(cost: Effect, effect: Effect): OptionalCostEffect =
        MiscPatterns.mayPay(cost, effect)

    fun mayPayOrElse(cost: Effect, ifPaid: Effect, ifNotPaid: Effect): OptionalCostEffect =
        MiscPatterns.mayPayOrElse(cost, ifPaid, ifNotPaid)

    // =========================================================================
    // Forage Patterns (MiscPatterns)
    // =========================================================================

    fun forage(afterEffect: Effect? = null): ChooseActionEffect =
        MiscPatterns.forage(afterEffect)

    // =========================================================================
    // Sacrifice Patterns (MiscPatterns)
    // =========================================================================

    fun sacrificeFor(filter: GameObjectFilter, countName: String, thenEffect: Effect): CompositeEffect =
        MiscPatterns.sacrificeFor(filter, countName, thenEffect)

    fun sacrifice(filter: GameObjectFilter, count: Int = 1, then: Effect): CompositeEffect =
        MiscPatterns.sacrifice(filter, count, then)

    // =========================================================================
    // Reflexive Trigger Patterns (MiscPatterns)
    // =========================================================================

    fun reflexiveTrigger(action: Effect, whenYouDo: Effect, optional: Boolean = true): ReflexiveTriggerEffect =
        MiscPatterns.reflexiveTrigger(action, whenYouDo, optional)

    // =========================================================================
    // Store and Reference Patterns (MiscPatterns)
    // =========================================================================

    fun storeEntity(effect: Effect, `as`: String): StoreResultEffect =
        MiscPatterns.storeEntity(effect, `as`)

    fun storeCount(effect: Effect, `as`: String): StoreCountEffect =
        MiscPatterns.storeCount(effect, `as`)

    // =========================================================================
    // Composite Patterns (MiscPatterns)
    // =========================================================================

    fun sequence(vararg effects: Effect): CompositeEffect =
        MiscPatterns.sequence(*effects)

    // =========================================================================
    // Drain (MiscPatterns)
    // =========================================================================

    fun drain(amount: Int, target: EffectTarget): CompositeEffect =
        MiscPatterns.drain(amount, target)

    // =========================================================================
    // Discard Patterns (HandPatterns)
    // =========================================================================

    fun eachOpponentDiscards(count: Int, controllerDrawsPerDiscard: Int = 0): Effect =
        HandPatterns.eachOpponentDiscards(count, controllerDrawsPerDiscard)

    fun discardCards(count: Int, target: EffectTarget = EffectTarget.Controller): CompositeEffect =
        HandPatterns.discardCards(count, target)

    fun discardRandom(count: Int, target: EffectTarget = EffectTarget.Controller): CompositeEffect =
        HandPatterns.discardRandom(count, target)

    fun discardHand(target: EffectTarget = EffectTarget.Controller): CompositeEffect =
        HandPatterns.discardHand(target)

    fun exileFromHand(count: Int = 1, target: EffectTarget = EffectTarget.ContextTarget(0)): CompositeEffect =
        HandPatterns.exileFromHand(count, target)

    // =========================================================================
    // Hand-to-Zone Patterns (HandPatterns)
    // =========================================================================

    fun putFromHand(filter: GameObjectFilter = GameObjectFilter.Any, count: Int = 1, entersTapped: Boolean = false): CompositeEffect =
        HandPatterns.putFromHand(filter, count, entersTapped)

    fun eachOpponentMayPutFromHand(filter: GameObjectFilter = GameObjectFilter.Any): ForEachPlayerEffect =
        HandPatterns.eachOpponentMayPutFromHand(filter)

    // =========================================================================
    // Wheel / Draw Patterns (HandPatterns)
    // =========================================================================

    fun wheelEffect(players: Player = Player.Each): ForEachPlayerEffect =
        HandPatterns.wheelEffect(players)

    fun eachPlayerDiscardsDraws(controllerBonusDraw: Int = 0): CompositeEffect =
        HandPatterns.eachPlayerDiscardsDraws(controllerBonusDraw)

    fun eachPlayerDrawsX(includeController: Boolean = true, includeOpponents: Boolean = true): ForEachPlayerEffect =
        HandPatterns.eachPlayerDrawsX(includeController, includeOpponents)

    fun eachPlayerMayDraw(maxCards: Int, lifePerCardNotDrawn: Int = 0): ForEachPlayerEffect =
        HandPatterns.eachPlayerMayDraw(maxCards, lifePerCardNotDrawn)

    fun loot(draw: Int = 1, discard: Int = 1): CompositeEffect =
        HandPatterns.loot(draw, discard)

    fun headGames(target: EffectTarget = EffectTarget.ContextTarget(0)): CompositeEffect =
        HandPatterns.headGames(target)

    // =========================================================================
    // Library Patterns (LibraryPatterns)
    // =========================================================================

    fun lookAtTopAndKeep(
        count: Int,
        keepCount: Int,
        keepDestination: CardDestination = CardDestination.ToZone(Zone.HAND),
        restDestination: CardDestination = CardDestination.ToZone(Zone.GRAVEYARD),
        revealed: Boolean = false
    ): CompositeEffect =
        LibraryPatterns.lookAtTopAndKeep(count, keepCount, keepDestination, restDestination, revealed)

    fun lookAtTopAndReorder(count: Int): CompositeEffect =
        LibraryPatterns.lookAtTopAndReorder(count)

    fun lookAtTopAndReorder(count: DynamicAmount): CompositeEffect =
        LibraryPatterns.lookAtTopAndReorder(count)

    fun scry(count: Int): CompositeEffect =
        LibraryPatterns.scry(count)

    fun surveil(count: Int): CompositeEffect =
        LibraryPatterns.surveil(count)

    fun searchLibrary(
        filter: GameObjectFilter = GameObjectFilter.Any,
        count: Int = 1,
        destination: SearchDestination = SearchDestination.HAND,
        entersTapped: Boolean = false,
        shuffleAfter: Boolean = true,
        reveal: Boolean = false
    ): CompositeEffect =
        LibraryPatterns.searchLibrary(filter, count, destination, entersTapped, shuffleAfter, reveal)

    fun searchMultipleZones(
        zones: List<Zone>,
        filter: GameObjectFilter = GameObjectFilter.Any,
        count: Int = 1,
        destination: SearchDestination = SearchDestination.BATTLEFIELD,
        entersTapped: Boolean = false
    ): CompositeEffect =
        LibraryPatterns.searchMultipleZones(zones, filter, count, destination, entersTapped)

    fun searchLibraryNthFromTop(
        filter: GameObjectFilter = GameObjectFilter.Any,
        positionFromTop: Int = 2
    ): CompositeEffect =
        LibraryPatterns.searchLibraryNthFromTop(filter, positionFromTop)

    fun lookAtTargetLibraryAndDiscard(count: Int, toGraveyard: Int = 1): CompositeEffect =
        LibraryPatterns.lookAtTargetLibraryAndDiscard(count, toGraveyard)

    fun searchTargetLibraryExile(count: Int = 1, filter: GameObjectFilter = GameObjectFilter.Any): CompositeEffect =
        LibraryPatterns.searchTargetLibraryExile(count, filter)

    fun revealUntilNonlandDealDamage(target: EffectTarget): CompositeEffect =
        LibraryPatterns.revealUntilNonlandDealDamage(target)

    fun revealUntilNonlandDealDamageEachTarget(): ForEachTargetEffect =
        LibraryPatterns.revealUntilNonlandDealDamageEachTarget()

    fun revealUntilNonlandModifyStats(): CompositeEffect =
        LibraryPatterns.revealUntilNonlandModifyStats()

    fun revealUntilCreatureTypeToBattlefield(): CompositeEffect =
        LibraryPatterns.revealUntilCreatureTypeToBattlefield()

    fun revealAndOpponentChooses(count: Int, filter: GameObjectFilter): CompositeEffect =
        LibraryPatterns.revealAndOpponentChooses(count, filter)

    fun mill(count: Int, target: EffectTarget = EffectTarget.Controller): CompositeEffect =
        LibraryPatterns.mill(count, target)

    fun mill(count: DynamicAmount, target: EffectTarget = EffectTarget.Controller): CompositeEffect =
        LibraryPatterns.mill(count, target)

    fun shuffleGraveyardIntoLibrary(target: EffectTarget = EffectTarget.ContextTarget(0)): CompositeEffect =
        LibraryPatterns.shuffleGraveyardIntoLibrary(target)

    fun lookAtTopXAndPutOntoBattlefield(
        countSource: DynamicAmount,
        filter: GameObjectFilter,
        shuffleAfter: Boolean = true
    ): CompositeEffect =
        LibraryPatterns.lookAtTopXAndPutOntoBattlefield(countSource, filter, shuffleAfter)

    // =========================================================================
    // Creature Type Patterns (CreatureTypePatterns)
    // =========================================================================

    fun chooseCreatureTypeRevealTop(): CompositeEffect =
        CreatureTypePatterns.chooseCreatureTypeRevealTop()

    fun chooseCreatureTypeReturnFromGraveyard(count: Int): CompositeEffect =
        CreatureTypePatterns.chooseCreatureTypeReturnFromGraveyard(count)

    fun chooseCreatureTypeShuffleGraveyardIntoLibrary(): CompositeEffect =
        CreatureTypePatterns.chooseCreatureTypeShuffleGraveyardIntoLibrary()

    fun chooseCreatureTypeModifyStats(
        powerModifier: DynamicAmount,
        toughnessModifier: DynamicAmount,
        duration: Duration = Duration.EndOfTurn,
        grantKeyword: Keyword? = null
    ): CompositeEffect =
        CreatureTypePatterns.chooseCreatureTypeModifyStats(powerModifier, toughnessModifier, duration, grantKeyword)

    fun chooseCreatureTypeUntap(): CompositeEffect =
        CreatureTypePatterns.chooseCreatureTypeUntap()

    fun becomeChosenTypeAllCreatures(
        excludedTypes: List<String> = emptyList(),
        controllerOnly: Boolean = false,
        duration: Duration = Duration.EndOfTurn
    ): CompositeEffect =
        CreatureTypePatterns.becomeChosenTypeAllCreatures(excludedTypes, controllerOnly, duration)

    fun chooseCreatureTypeGainControl(duration: Duration = Duration.Permanent): CompositeEffect =
        CreatureTypePatterns.chooseCreatureTypeGainControl(duration)

    fun chooseCreatureTypeMustAttack(): CompositeEffect =
        CreatureTypePatterns.chooseCreatureTypeMustAttack()

    fun patriarchsBidding(): CompositeEffect =
        CreatureTypePatterns.patriarchsBidding()

    fun destroyAllExceptStoredSubtypes(
        noRegenerate: Boolean = false,
        exceptSubtypesFromStored: String
    ): CompositeEffect =
        CreatureTypePatterns.destroyAllExceptStoredSubtypes(noRegenerate, exceptSubtypesFromStored)

    fun destroyAllSharingTypeWithSacrificed(noRegenerate: Boolean = false): CompositeEffect =
        CreatureTypePatterns.destroyAllSharingTypeWithSacrificed(noRegenerate)

    /**
     * Cryptic Gateway pipeline: gather subtypes of permanents tapped as cost, then let
     * the controller pick a creature from hand that shares a type with each of them.
     *
     * Pipeline: TappedAsCost → GatherSubtypes → GatherCards(hand, sharing filter) →
     * Select(up to 1) → Move(battlefield)
     */
    fun putCreatureFromHandSharingTypeWithTapped(): CompositeEffect = CompositeEffect(listOf(
        GatherCardsEffect(
            source = CardSource.TappedAsCost,
            storeAs = "tappedPermanents"
        ),
        GatherSubtypesEffect(
            from = "tappedPermanents",
            storeAs = "tappedSubtypes"
        ),
        GatherCardsEffect(
            source = CardSource.FromZone(
                zone = Zone.HAND,
                player = Player.You,
                filter = GameObjectFilter.Creature.withSubtypeInEachStoredGroup("tappedSubtypes")
            ),
            storeAs = "candidates"
        ),
        SelectFromCollectionEffect(
            from = "candidates",
            selection = SelectionMode.ChooseUpTo(DynamicAmount.Fixed(1)),
            storeSelected = "chosen",
            prompt = "You may put a creature card from your hand onto the battlefield"
        ),
        MoveCollectionEffect(
            from = "chosen",
            destination = CardDestination.ToZone(Zone.BATTLEFIELD)
        )
    ))

    // =========================================================================
    // Exile Patterns (ExilePatterns)
    // =========================================================================

    fun exileUntilLeaves(exileTarget: EffectTarget, variableName: String = "exiledCard"): StoreResultEffect =
        ExilePatterns.exileUntilLeaves(exileTarget, variableName)

    fun exileUntilEndStep(target: EffectTarget): Effect =
        ExilePatterns.exileUntilEndStep(target)

    fun searchAndExileLinked(count: Int = 7, filter: GameObjectFilter = GameObjectFilter.Any): CompositeEffect =
        ExilePatterns.searchAndExileLinked(count, filter)

    fun exileGroupAndLink(filter: GroupFilter, storeAs: String = "linked_exile"): CompositeEffect =
        ExilePatterns.exileGroupAndLink(filter, storeAs)

    fun returnLinkedExile(underOwnersControl: Boolean = false, storeAs: String = "linked_return"): CompositeEffect =
        ExilePatterns.returnLinkedExile(underOwnersControl, storeAs)

    fun takeFromLinkedExile(storeAs: String = "linked_take"): CompositeEffect =
        ExilePatterns.takeFromLinkedExile(storeAs)

    fun shuffleAndExileTopPlayFree(): Effect =
        ExilePatterns.shuffleAndExileTopPlayFree()

    fun exileAndReplaceWithToken(
        target: EffectTarget,
        power: Int,
        toughness: Int,
        colors: Set<Color> = emptySet(),
        creatureTypes: Set<String>,
        keywords: Set<Keyword> = emptySet()
    ): CompositeEffect =
        ExilePatterns.exileAndReplaceWithToken(target, power, toughness, colors, creatureTypes, keywords)

    fun destroyAndReplaceWithToken(
        target: EffectTarget,
        power: Int,
        toughness: Int,
        colors: Set<Color> = emptySet(),
        creatureTypes: Set<String>,
        keywords: Set<Keyword> = emptySet()
    ): CompositeEffect =
        ExilePatterns.destroyAndReplaceWithToken(target, power, toughness, colors, creatureTypes, keywords)

    fun eachPlayerRevealCreaturesCreateTokens(
        tokenPower: Int,
        tokenToughness: Int,
        tokenColors: Set<Color>,
        tokenCreatureTypes: Set<String>,
        tokenImageUri: String? = null
    ): ForEachPlayerEffect =
        ExilePatterns.eachPlayerRevealCreaturesCreateTokens(tokenPower, tokenToughness, tokenColors, tokenCreatureTypes, tokenImageUri)

    // =========================================================================
    // Group Effect Patterns (GroupPatterns)
    // =========================================================================

    fun untapGroup(filter: GroupFilter = GroupFilter.AllCreatures): ForEachInGroupEffect =
        GroupPatterns.untapGroup(filter)

    fun tapAll(filter: GroupFilter): ForEachInGroupEffect =
        GroupPatterns.tapAll(filter)

    fun returnAllToHand(filter: GroupFilter): CompositeEffect =
        GroupPatterns.returnAllToHand(filter)

    fun destroyAll(filter: GroupFilter, noRegenerate: Boolean = false): ForEachInGroupEffect =
        GroupPatterns.destroyAll(filter, noRegenerate)

    fun destroyAllPipeline(
        filter: GameObjectFilter,
        noRegenerate: Boolean = false,
        storeDestroyedAs: String? = null
    ): CompositeEffect =
        GroupPatterns.destroyAllPipeline(filter, noRegenerate, storeDestroyedAs)

    fun destroyAllAndAttachedPipeline(filter: GameObjectFilter, noRegenerate: Boolean = false): CompositeEffect =
        GroupPatterns.destroyAllAndAttachedPipeline(filter, noRegenerate)

    fun grantKeywordToAll(keyword: Keyword, filter: GroupFilter, duration: Duration = Duration.EndOfTurn): ForEachInGroupEffect =
        GroupPatterns.grantKeywordToAll(keyword, filter, duration)

    fun removeKeywordFromAll(keyword: Keyword, filter: GroupFilter, duration: Duration = Duration.EndOfTurn): ForEachInGroupEffect =
        GroupPatterns.removeKeywordFromAll(keyword, filter, duration)

    fun modifyStatsForAll(power: Int, toughness: Int, filter: GroupFilter, duration: Duration = Duration.EndOfTurn): ForEachInGroupEffect =
        GroupPatterns.modifyStatsForAll(power, toughness, filter, duration)

    fun modifyStatsForAll(power: DynamicAmount, toughness: DynamicAmount, filter: GroupFilter, duration: Duration = Duration.EndOfTurn): ForEachInGroupEffect =
        GroupPatterns.modifyStatsForAll(power, toughness, filter, duration)

    fun dealDamageToAll(amount: Int, filter: GroupFilter): ForEachInGroupEffect =
        GroupPatterns.dealDamageToAll(amount, filter)

    fun dealDamageToAll(amount: DynamicAmount, filter: GroupFilter): ForEachInGroupEffect =
        GroupPatterns.dealDamageToAll(amount, filter)

    fun gainControlOfGroup(filter: GroupFilter = GroupFilter.AllCreatures, duration: Duration = Duration.EndOfTurn): ForEachInGroupEffect =
        GroupPatterns.gainControlOfGroup(filter, duration)

    // =========================================================================
    // Multi-Player Utility Patterns (MiscPatterns)
    // =========================================================================

    fun eachPlayerReturnsPermanentToHand(): ForEachPlayerEffect =
        MiscPatterns.eachPlayerReturnsPermanentToHand()

    fun eachPlayerSearchesLibrary(filter: GameObjectFilter, count: DynamicAmount): ForEachPlayerEffect =
        MiscPatterns.eachPlayerSearchesLibrary(filter, count)
}
