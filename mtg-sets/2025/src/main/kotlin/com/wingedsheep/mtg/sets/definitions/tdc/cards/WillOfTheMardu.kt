package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Will of the Mardu — Tarkir: Dragonstorm Commander #17 (canonical printing)
 * {2}{W} · Instant
 *
 * Choose one. If you control a commander as you cast this spell, you may choose both instead.
 * • Create a number of 1/1 red Warrior creature tokens equal to the number of creatures target
 *   player controls.
 * • Will of the Mardu deals damage to target creature equal to the number of creatures you control.
 *
 * The Jeska's Will conditional mode count (`minChooseCount = 1`, capped at 2 while
 * [Conditions.YouControlACommander] holds as the spell is cast). Both counts are read on
 * resolution; modes resolve in printed order, so with both chosen the Warriors from mode one
 * already count toward mode two's damage.
 */
val WillOfTheMardu = card("Will of the Mardu") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Instant"
    oracleText = "Choose one. If you control a commander as you cast this spell, you may choose both instead.\n" +
        "• Create a number of 1/1 red Warrior creature tokens equal to the number of creatures target player controls.\n" +
        "• Will of the Mardu deals damage to target creature equal to the number of creatures you control."

    spell {
        modal(
            chooseCount = 2,
            minChooseCount = 1,
            dynamicChooseCount = DynamicAmounts.conditional(
                condition = Conditions.YouControlACommander,
                ifTrue = 2,
                ifFalse = 1
            )
        ) {
            mode("Create a 1/1 red Warrior token for each creature target player controls") {
                val player = target(Targets.Player)
                effect = Effects.CreateToken(
                    count = DynamicAmounts.battlefield(player.asPlayer, GameObjectFilter.Creature).count(),
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.RED),
                    creatureTypes = setOf("Warrior"),
                    imageUri = "https://cards.scryfall.io/normal/front/7/e/7edc0515-a130-45a7-aa09-0e23bba41587.jpg?1783906783",
                )
            }
            mode("Deal damage to target creature equal to the number of creatures you control") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.DealDamage(DynamicAmounts.creaturesYouControl(), creature)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "17"
        artist = "Tuan Duong Chu"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/26f6b519-20ff-4255-926d-5caee6c572d8.jpg?1783907183"
        ruling("2025-04-04", "The commander you control doesn't have to be your commander.")
        ruling(
            "2025-04-04",
            "Whether or not you control a commander is determined only once, as you choose the modes for this " +
                "spell. If you somehow lose control of that commander before you finish casting the spell " +
                "(perhaps because you sacrifice it to activate a mana ability), it won't change the number of " +
                "modes chosen."
        )
    }
}
