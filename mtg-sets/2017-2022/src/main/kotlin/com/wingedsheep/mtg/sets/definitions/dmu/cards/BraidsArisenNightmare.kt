package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Braids, Arisen Nightmare
 * {1}{B}{B}
 * Legendary Creature — Nightmare
 * 3/3
 * At the beginning of your end step, you may sacrifice an artifact, creature, enchantment, land, or
 * planeswalker. If you do, each opponent may sacrifice a permanent of their choice that shares a
 * card type with it. For each opponent who doesn't, that player loses 2 life and you draw a card.
 *
 * "It" is the sacrificed permanent as it last existed on the battlefield: `SacrificedAsCost()` reads
 * the snapshot the sacrifice step captured, so a sacrificed token or an animated land still has its
 * card types to share. Each opponent who sacrifices nothing is recorded per iteration; the life loss
 * and the draws sit outside the loop so "you" stays Braids's controller.
 */
val BraidsArisenNightmare = card("Braids, Arisen Nightmare") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Nightmare"
    oracleText = "At the beginning of your end step, you may sacrifice an artifact, creature, " +
        "enchantment, land, or planeswalker. If you do, each opponent may sacrifice a permanent of " +
        "their choice that shares a card type with it. For each opponent who doesn't, that player " +
        "loses 2 life and you draw a card."
    power = 3
    toughness = 3

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.Pipeline {
            val yours = gather(
                GameObjectFilter.Artifact or GameObjectFilter.Creature or GameObjectFilter.Enchantment or
                    GameObjectFilter.Land or GameObjectFilter.Planeswalker,
                Player.You,
            )
            val sacrificed = chooseUpTo(
                1,
                from = yours,
                useTargetingUI = true,
                prompt = "You may sacrifice an artifact, creature, enchantment, land, or planeswalker",
                selectedLabel = "Sacrifice",
            )
            sacrifice(sacrificed)
            ifNotEmpty(sacrificed) {
                val refused = forEachPlayerCollecting(Player.EachOpponent) {
                    val theirs = gather(
                        GameObjectFilter.Permanent.sharingCardTypeWith(EffectTarget.SacrificedAsCost()),
                        Player.You,
                    )
                    val answer = chooseUpTo(
                        1,
                        from = theirs,
                        useTargetingUI = true,
                        prompt = "You may sacrifice a permanent that shares a card type with the " +
                            "sacrificed permanent, or lose 2 life",
                        selectedLabel = "Sacrifice",
                    )
                    sacrifice(answer)
                    listOf(storePlayer(onlyIf = Conditions.Not(Conditions.CollectionContainsMatch(answer))))
                }.single()
                run(Effects.ForEachPlayer(refused.asPlayers, Effects.LoseLife(2, EffectTarget.Controller)))
                run(Effects.DrawCards(refused.count))
            }
        }
        description = "At the beginning of your end step, you may sacrifice an artifact, creature, " +
            "enchantment, land, or planeswalker. If you do, each opponent may sacrifice a permanent " +
            "of their choice that shares a card type with it. For each opponent who doesn't, that " +
            "player loses 2 life and you draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "84"
        artist = "Heonhwa"
        imageUri = "https://cards.scryfall.io/normal/front/4/f/4ff97c69-6a6b-401c-b0a1-55fa81045d19.jpg?1783921336"

        ruling(
            "2022-09-09",
            "If the permanent that you sacrifice as you resolve Braids's triggered ability has more " +
                "than one card type, each opponent may choose to sacrifice any permanent they control " +
                "that shares any card type with it. For example, if the permanent you sacrifice is an " +
                "artifact creature, one opponent might choose to sacrifice an artifact and another " +
                "opponent might choose to sacrifice a creature."
        )
    }
}
