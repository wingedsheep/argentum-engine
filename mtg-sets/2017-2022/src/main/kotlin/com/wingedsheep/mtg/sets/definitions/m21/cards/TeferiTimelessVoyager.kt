package com.wingedsheep.mtg.sets.definitions.m21.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Teferi, Timeless Voyager
 * {4}{U}{U}
 * Legendary Planeswalker — Teferi
 * +1: Draw a card.
 * −3: Put target creature on top of its owner's library.
 * −8: Each creature target opponent controls phases out. Until the end of your next turn, they
 * can't phase in.
 *
 * The −8's lock is a phase-in window, not a delayed phase-in: once your next turn ends the
 * creatures phase in at their controller's next untap step, as the ruling says.
 */
val TeferiTimelessVoyager = card("Teferi, Timeless Voyager") {
    manaCost = "{4}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Planeswalker — Teferi"
    startingLoyalty = 4
    oracleText = "+1: Draw a card.\n" +
        "−3: Put target creature on top of its owner's library.\n" +
        "−8: Each creature target opponent controls phases out. Until the end of your next turn, " +
        "they can't phase in. (Treat them and anything attached to them as though they don't exist.)"

    loyaltyAbility(+1) {
        effect = Effects.DrawCards(1)
    }

    loyaltyAbility(-3) {
        val creature = target(TargetFilter.Creature)
        effect = Effects.PutOnTopOfLibrary(creature)
    }

    loyaltyAbility(-8) {
        target(Targets.Opponent)
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Creature.targetOpponentControls()),
            Effects.PhaseOut(EffectTarget.IterationEntity, cantPhaseIn = Duration.EndOfYourNextTurn)
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "324"
        artist = "Jake Murray"
        imageUri = "https://cards.scryfall.io/normal/front/6/2/62d05287-ad7f-4de8-9f68-8f36c62fae72.jpg?1783930622"
        ruling("2020-06-23", "Normally, phased-out permanents phase in during their controller's untap step. Teferi's last ability causes the phased-out permanents to not phase in until the duration has expired; they'll phase in during their controller's next untap step after your next turn.")
        ruling("2020-06-23", "While a permanent is phased out, it's treated as though it doesn't exist. It can't be the target of spells or abilities, its static abilities have no effect on the game, its triggered abilities can't trigger, it can't attack or block, and so on.")
        ruling("2020-06-23", "Each Aura and Equipment attached to a permanent that's phasing out also phases out. They will phase in with that permanent and still be attached to it. Similarly, permanents that phase out with counters phase in with those counters.")
        ruling("2020-06-23", "In a multiplayer game, if you leave the game after Teferi's last ability resolves but before your next turn ends, its effect lasts until your next turn would have begun. It neither expires immediately nor lasts indefinitely.")
    }
}
