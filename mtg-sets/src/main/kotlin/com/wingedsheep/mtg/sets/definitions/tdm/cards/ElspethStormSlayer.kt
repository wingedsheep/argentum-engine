package com.wingedsheep.mtg.sets.definitions.tdm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MultiplyTokenCreation
import com.wingedsheep.sdk.scripting.effects.AddCountersEffect
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Elspeth, Storm Slayer — Tarkir: Dragonstorm #11
 * {3}{W}{W} · Legendary Planeswalker — Elspeth · Loyalty 5
 *
 * If one or more tokens would be created under your control, twice that many of those tokens
 * are created instead.
 * +1: Create a 1/1 white Soldier creature token.
 * 0: Put a +1/+1 counter on each creature you control. Those creatures gain flying until your
 *    next turn.
 * −3: Destroy target creature an opponent controls with mana value 3 or greater.
 */
val ElspethStormSlayer = card("Elspeth, Storm Slayer") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Planeswalker — Elspeth"
    startingLoyalty = 5
    oracleText = "If one or more tokens would be created under your control, twice that many of " +
        "those tokens are created instead.\n" +
        "+1: Create a 1/1 white Soldier creature token.\n" +
        "0: Put a +1/+1 counter on each creature you control. Those creatures gain flying until " +
        "your next turn.\n" +
        "−3: Destroy target creature an opponent controls with mana value 3 or greater."

    // If one or more tokens would be created under your control, twice that many instead.
    replacementEffect(MultiplyTokenCreation())

    // +1: Create a 1/1 white Soldier creature token.
    loyaltyAbility(+1) {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Soldier"),
            imageUri = "https://cards.scryfall.io/normal/front/6/4/6455d903-6996-448f-9148-9068febecb00.jpg?1742506671"
        )
    }

    // 0: Put a +1/+1 counter on each creature you control. Those creatures gain flying until
    // your next turn.
    loyaltyAbility(0) {
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreaturesYouControl,
            effect = AddCountersEffect(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self)
        ).then(
            Effects.ForEachInGroup(
                filter = GroupFilter.AllCreaturesYouControl,
                effect = GrantKeywordEffect(Keyword.FLYING, EffectTarget.Self, Duration.UntilYourNextTurn)
            )
        )
    }

    // −3: Destroy target creature an opponent controls with mana value 3 or greater.
    loyaltyAbility(-3) {
        val victim = target(
            "creature an opponent controls with mana value 3 or greater",
            TargetObject(filter = TargetFilter(GameObjectFilter.Creature.opponentControls().manaValueAtLeast(3)))
        )
        effect = Effects.Destroy(victim)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "11"
        artist = "Ekaterina Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/7/3/73a065e3-b530-4e62-ab3c-4f6f908184ec.jpg?1743203994"
    }
}
