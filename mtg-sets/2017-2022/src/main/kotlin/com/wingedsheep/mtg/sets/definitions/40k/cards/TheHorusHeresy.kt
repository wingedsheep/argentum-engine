package com.wingedsheep.mtg.sets.definitions.`40k`.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.predicates.ControllerPredicate
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Horus Heresy
 * {3}{U}{B}{R}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — For each opponent, gain control of up to one target nonlegendary creature that player
 * controls for as long as this Saga remains on the battlefield.
 * II — Draw a card for each creature you control but don't own.
 * III — Starting with you, each player chooses a creature. Destroy each creature chosen this way.
 *
 * Chapter I is In the Darkness Bind Them's one-per-opponent distribution (`dynamicMaxCount` over
 * the opponents + `differentControllers`), each steal riding [Duration.WhileSourceOnBattlefield]
 * keyed to the Saga — so the creatures go home when the Saga is sacrificed after chapter III.
 * Chapter II counts creatures you control whose owner isn't you (`Not(OwnedByYou)`), at
 * resolution. Chapter III is Eureka's "starting with you" rotation: each player in turn picks any
 * creature on the battlefield (not a target), and every creature picked is destroyed together.
 */
val TheHorusHeresy = card("The Horus Heresy") {
    manaCost = "{3}{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — For each opponent, gain control of up to one target nonlegendary creature that player " +
        "controls for as long as this Saga remains on the battlefield.\n" +
        "II — Draw a card for each creature you control but don't own.\n" +
        "III — Starting with you, each player chooses a creature. Destroy each creature chosen this way."

    sagaChapter(1) {
        targets(
            TargetFilter(GameObjectFilter.Creature.nonlegendary().opponentControls()),
            optional = true,
            dynamicMaxCount = DynamicAmounts.playerCount(Player.EachOpponent),
            differentControllers = true,
        )
        effect = Effects.ForEachTarget(
            Effects.GainControl(
                EffectTarget.ContextTarget(0),
                Duration.WhileSourceOnBattlefield("The Horus Heresy"),
            ),
        )
    }

    sagaChapter(2) {
        effect = Effects.DrawCards(
            DynamicAmounts.battlefield(
                Player.You,
                GameObjectFilter.Creature.withControllerPredicate(
                    ControllerPredicate.Not(ControllerPredicate.OwnedByYou)
                ),
            ).count()
        )
    }

    sagaChapter(3) {
        effect = Effects.Pipeline {
            val (chosen) = forEachPlayerCollecting(Player.EachStartingWithYou) {
                val creatures = gather(CardSource.BattlefieldMatching(filter = GameObjectFilter.Creature))
                listOf(
                    chooseExactly(
                        1,
                        from = creatures,
                        chooser = Chooser.Controller,
                        useTargetingUI = true,
                        prompt = "Choose a creature to destroy",
                    )
                )
            }
            destroy(chosen)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "126"
        artist = "Roman Tishenin"
        imageUri = "https://cards.scryfall.io/normal/front/c/9/c9551a51-2bbd-425b-a634-185921a96865.jpg?1783920846"
    }
}
