package com.wingedsheep.mtg.sets.definitions.trk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Supertype
import com.wingedsheep.sdk.dsl.CollectionSlot
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gray and Adira, Forever Joined — Star Trek #238
 * {2}{U/R}{U/R} · Legendary Creature — Trill Human Officer · 2/3
 *
 * At the beginning of combat on your turn, if you've cast a noncreature spell this turn, create a
 * token that's a copy of target creature you control, except it isn't legendary. It gains haste
 * until end of turn. Sacrifice it at the beginning of the next end step.
 *
 * "Isn't legendary" is a copiable exception (`removedSupertypes`); the haste is a non-copiable
 * until-end-of-turn grant on the created token, as Jaxis, the Troublemaker models its grant.
 */
val GrayAndAdiraForeverJoined = card("Gray and Adira, Forever Joined") {
    manaCost = "{2}{U/R}{U/R}"
    colorIdentity = "UR"
    typeLine = "Legendary Creature — Trill Human Officer"
    power = 2
    toughness = 3
    oracleText = "At the beginning of combat on your turn, if you've cast a noncreature spell this turn, " +
        "create a token that's a copy of target creature you control, except it isn't legendary. It gains " +
        "haste until end of turn. Sacrifice it at the beginning of the next end step."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.BEGIN_COMBAT)
        interveningIf = Conditions.YouCastSpellsThisTurn(atLeast = 1, filter = GameObjectFilter.Noncreature)
        val creature = target(TargetFilter(GameObjectFilter.Creature.youControl()))
        effect = Effects.CreateTokenCopyOfTarget(
            target = creature,
            removedSupertypes = setOf(Supertype.LEGENDARY),
            sacrificeAtStep = Step.END,
        ) then Effects.ForEachInCollection(
            CollectionSlot.CreatedTokens,
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.IterationEntity, Duration.EndOfTurn)
        )
        description = "At the beginning of combat on your turn, if you've cast a noncreature spell this " +
            "turn, create a token that's a copy of target creature you control, except it isn't legendary. " +
            "It gains haste until end of turn. Sacrifice it at the beginning of the next end step."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "238"
        artist = "Julia Metzger"
        flavorText = "\"Tal accepted each of us. And while a human joining is unusual, Tal accepts you as well.\"\n—Senna Tal"
        imageUri = "https://cards.scryfall.io/normal/front/e/e/eede6822-c8bc-4344-81a9-bc608245d5b6.jpg?1792508333"
    }
}
