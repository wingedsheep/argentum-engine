package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Sunder the Gateway — March of the Machine #39
 * {1}{W} · Sorcery
 *
 * Choose one —
 * • Destroy target nontoken artifact or enchantment an opponent controls. Incubate 2.
 * • Incubate 2, then transform an Incubator token you control.
 *
 * The second mode's Incubator is *chosen* on resolution, not targeted (ruling 2023-04-14), so it
 * is a battlefield gather of Incubator tokens you control — including the one just created —
 * followed by an exactly-one choice and a transform of the chosen token.
 */
val SunderTheGateway = card("Sunder the Gateway") {
    manaCost = "{1}{W}"
    colorIdentity = "W"
    typeLine = "Sorcery"
    oracleText = "Choose one —\n" +
        "• Destroy target nontoken artifact or enchantment an opponent controls. Incubate 2. " +
        "(Create an Incubator token with two +1/+1 counters on it and \"{2}: Transform this token.\" " +
        "It transforms into a 0/0 Phyrexian artifact creature.)\n" +
        "• Incubate 2, then transform an Incubator token you control."

    spell {
        modal(chooseCount = 1) {
            mode("Destroy target nontoken artifact or enchantment an opponent controls. Incubate 2") {
                val permanent = target(
                    TargetFilter(GameObjectFilter.ArtifactOrEnchantment.nontoken().opponentControls())
                )
                effect = Effects.Destroy(permanent) then Effects.Incubate(2)
            }
            mode("Incubate 2, then transform an Incubator token you control") {
                effect = Effects.Incubate(2) then Effects.Pipeline {
                    val incubators = gather(
                        GameObjectFilter.Permanent.withSubtype("Incubator").token(),
                        player = Player.You
                    )
                    val chosen = chooseExactly(
                        1,
                        from = incubators,
                        prompt = "Choose an Incubator token you control to transform",
                        useTargetingUI = true
                    )
                    ifNotEmpty(chosen) {
                        run(Effects.Transform(chosen.asTarget(0)))
                    }
                }
            }
        }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "39"
        artist = "Titus Lunter"
        imageUri = "https://cards.scryfall.io/normal/front/1/9/194ad785-21bb-4ce6-8776-31c67259fb99.jpg?1783917049"
        ruling(
            "2023-04-14",
            "For the first mode, if the nontoken artifact or enchantment is an illegal target at the " +
                "time Sunder the Gateway tries to resolve, it won't resolve and none of its effects will " +
                "happen. You won't incubate."
        )
        ruling(
            "2023-04-14",
            "For the second mode, the Incubator token is chosen as Sunder the Gateway resolves. You can " +
                "transform the Incubator token you just created or one you already controlled."
        )
    }
}
