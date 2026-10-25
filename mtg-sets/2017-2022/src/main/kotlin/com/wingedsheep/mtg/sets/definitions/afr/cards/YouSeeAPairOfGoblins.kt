package com.wingedsheep.mtg.sets.definitions.afr.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * You See a Pair of Goblins
 * {2}{R}
 * Instant
 *
 * Choose one —
 * • Charge Them — Creatures you control get +2/+0 until end of turn.
 * • Befriend Them — Create two 1/1 red Goblin creature tokens.
 *
 * A choose-one `modal(chooseCount = 1)`: a group +2/+0 pump over the creatures you control at
 * resolution, or two of the set's 1/1 red Goblin tokens (Swarming Goblins' token art).
 */
val YouSeeAPairOfGoblins = card("You See a Pair of Goblins") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Instant"
    oracleText = "Choose one —\n" +
        "• Charge Them — Creatures you control get +2/+0 until end of turn.\n" +
        "• Befriend Them — Create two 1/1 red Goblin creature tokens."

    spell {
        modal(chooseCount = 1) {
            mode("Charge Them — Creatures you control get +2/+0 until end of turn.") {
                effect = Effects.ForEachInGroup(
                    GroupFilter.AllCreaturesYouControl,
                    Effects.ModifyStats(2, 0, EffectTarget.IterationEntity)
                )
            }
            mode("Befriend Them — Create two 1/1 red Goblin creature tokens.") {
                effect = Effects.CreateToken(
                    power = 1,
                    toughness = 1,
                    colors = setOf(Color.RED),
                    creatureTypes = setOf("Goblin"),
                    count = 2,
                    imageUri = "https://cards.scryfall.io/normal/front/1/4/1425e965-7eea-419c-a7ec-c8169fa9edbf.jpg?1783926342",
                )
            }
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "170"
        artist = "Aaron Miller"
        flavorText = "\"I have the shot. Shall I take it?\"\n—Varis, Silverymoon ranger"
        imageUri = "https://cards.scryfall.io/normal/front/b/6/b69e7200-96ea-4455-83cc-0a497d56efe5.jpg?1783926468"
    }
}
