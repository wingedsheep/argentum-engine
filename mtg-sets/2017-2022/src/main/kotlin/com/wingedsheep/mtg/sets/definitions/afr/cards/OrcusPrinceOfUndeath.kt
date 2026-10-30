package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.dsl.unaryMinus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.targets.TotalCap

/**
 * Orcus, Prince of Undeath
 * {X}{2}{B}{R}
 * Legendary Creature — Demon
 * 5/3
 * Flying, trample
 * When Orcus enters, choose one —
 * • Each other creature gets -X/-X until end of turn. You lose X life.
 * • Return up to X target creature cards with total mana value X or less from your graveyard to the
 *   battlefield. They gain haste until end of turn.
 *
 * X is the X paid to cast Orcus (`CastX`), read by the enters trigger. The second mode's "up to X" target
 * count is locked as the modal trigger goes on the stack, like a non-modal trigger's.
 */
val OrcusPrinceOfUndeath = card("Orcus, Prince of Undeath") {
    manaCost = "{X}{2}{B}{R}"
    colorIdentity = "BR"
    typeLine = "Legendary Creature — Demon"
    power = 5
    toughness = 3
    oracleText = "Flying, trample\n" +
        "When Orcus enters, choose one —\n" +
        "• Each other creature gets -X/-X until end of turn. You lose X life.\n" +
        "• Return up to X target creature cards with total mana value X or less from your graveyard " +
        "to the battlefield. They gain haste until end of turn."

    keywords(Keyword.FLYING, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        val negX = -DynamicAmounts.castX()
        effect = ModalEffect.chooseOne(
            mode("Each other creature gets -X/-X until end of turn. You lose X life.") {
                effect = Effects.ForEachInGroup(
                    Filters.Group.otherCreatures,
                    Effects.ModifyStats(negX, negX, EffectTarget.IterationEntity)
                ) then Effects.LoseLife(DynamicAmounts.castX(), EffectTarget.Controller)
            },
            mode(
                "Return up to X target creature cards with total mana value X or less from your graveyard " +
                    "to the battlefield. They gain haste until end of turn."
            ) {
                targets(
                    TargetFilter.CreatureInYourGraveyard,
                    optional = true,
                    dynamicMaxCount = DynamicAmounts.castX(),
                    totalAtMost = TotalCap.manaValue(DynamicAmounts.castX()),
                )
                effect = Effects.ForEachTarget(
                    Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.ContextTarget(0)) then
                        Effects.GrantKeyword(Keyword.HASTE, EffectTarget.ContextTarget(0))
                )
            },
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "229"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/91f5c6d3-fb04-4a2e-87b6-9ed2314085a8.jpg?1783926445"
        ruling(
            "2021-07-23",
            "You may cast Orcus, Prince of Undeath with X equal to 0. If you do, the triggered ability " +
                "will do nothing as it resolves."
        )
    }
}
