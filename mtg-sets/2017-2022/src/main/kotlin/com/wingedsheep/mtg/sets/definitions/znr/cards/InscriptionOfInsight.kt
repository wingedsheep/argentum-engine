package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Inscription of Insight — Zendikar Rising #61
 * {3}{U} · Sorcery
 *
 * Kicker {2}{U}{U}
 * Choose one. If this spell was kicked, choose any number instead.
 * • Return up to two target creatures to their owners' hands.
 * • Scry 2, then draw two cards.
 * • Target player creates an X/X blue Illusion creature token, where X is the number of cards in
 *   their hand.
 *
 * The kicked mode count is a cast-time `dynamicChooseCount` over [Conditions.WasKicked], which reads
 * the cast's own kicker declaration (CR 601.2b announces modes and kicker together): one mode
 * unkicked, one to three kicked. No mode can be chosen twice (default `allowRepeat = false`).
 *
 * X is read as the token mode is performed — after an earlier bounce mode has already put cards
 * into hands — and fixed at creation (`dynamicPower` snapshots), matching the rulings.
 */
val InscriptionOfInsight = card("Inscription of Insight") {
    manaCost = "{3}{U}"
    colorIdentity = "U"
    typeLine = "Sorcery"
    oracleText = "Kicker {2}{U}{U}\n" +
        "Choose one. If this spell was kicked, choose any number instead.\n" +
        "• Return up to two target creatures to their owners' hands.\n" +
        "• Scry 2, then draw two cards.\n" +
        "• Target player creates an X/X blue Illusion creature token, where X is the number of " +
        "cards in their hand."

    keywordAbility(KeywordAbility.kicker("{2}{U}{U}"))

    spell {
        modal(
            chooseCount = 3,
            minChooseCount = 1,
            dynamicChooseCount = DynamicAmounts.conditional(
                condition = Conditions.WasKicked,
                ifTrue = 3,
                ifFalse = 1
            )
        ) {
            mode("Return up to two target creatures to their owners' hands") {
                val creatures = targets(TargetFilter.Creature, count = 2, optional = true)
                effect = Effects.ForEachTargetOf(creatures, Effects.ReturnToHand(EffectTarget.IterationEntity))
            }
            mode("Scry 2, then draw two cards") {
                effect = Effects.Scry(2) then Effects.DrawCards(2)
            }
            mode("Target player creates an X/X blue Illusion creature token, where X is the number of cards in their hand") {
                val player = target(Targets.Player)
                val handSize = DynamicAmounts.count(player.asPlayer, Zone.HAND)
                effect = Effects.CreateDynamicToken(
                    dynamicPower = handSize,
                    dynamicToughness = handSize,
                    colors = setOf(Color.BLUE),
                    creatureTypes = setOf("Illusion"),
                    controller = player,
                    imageUri = "https://cards.scryfall.io/normal/front/2/3/2300635e-7771-4676-a5a5-29a9d8f49f1a.jpg?1783929499"
                )
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "61"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/2/8/281cdb1c-21ae-4430-8e0f-c993f52ba891.jpg?1783929397"
        ruling(
            "2020-09-25",
            "If more than one mode is chosen, perform them in the order written. Nothing can happen " +
                "in between, however, and no player may choose to take actions."
        )
        ruling(
            "2020-09-25",
            "The value of X is determined only as you perform the third mode while Inscription of " +
                "Insight is resolving. Once that happens, the value of X won't change even if the " +
                "number of cards in the player's hand changes."
        )
        ruling("2020-09-25", "If you kick Inscription of Insight, you can't choose any one mode more than once.")
    }
}
