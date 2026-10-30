package com.wingedsheep.mtg.sets.definitions.`40k`.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.times
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Birth of the Imperium
 * {2}{W}{U}{B}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Create a 2/2 white Astartes Warrior creature token with vigilance for each opponent you have.
 * II — Each opponent sacrifices a creature of their choice.
 * III — Draw two cards for each opponent who controls fewer creatures than you.
 *
 * Chapter I counts opponents with [DynamicAmounts.playerCount]. Chapter II is the untargeted
 * "each opponent sacrifices" of Dictate of Erebos. Chapter III is Wojek Investigator's
 * per-opponent count: [DynamicAmounts.countPlayersWith] rebinds the controller to each candidate
 * opponent, so `Player.You` inside the comparison is that opponent's creature count, while
 * [Player.ControllerOfSource] reads the Saga's controller off the Saga itself (which is still on
 * the battlefield while chapter III resolves — it's sacrificed only after the ability leaves the
 * stack). The comparison is strict: an opponent with as many creatures as you doesn't count.
 * Counts are read on resolution.
 */
val BirthOfTheImperium = card("Birth of the Imperium") {
    manaCost = "{2}{W}{U}{B}"
    colorIdentity = "WUB"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Create a 2/2 white Astartes Warrior creature token with vigilance for each opponent you have.\n" +
        "II — Each opponent sacrifices a creature of their choice.\n" +
        "III — Draw two cards for each opponent who controls fewer creatures than you."

    sagaChapter(1) {
        effect = Effects.CreateToken(
            count = DynamicAmounts.playerCount(Player.EachOpponent),
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Astartes", "Warrior"),
            keywords = setOf(Keyword.VIGILANCE),
            imageUri = "https://cards.scryfall.io/normal/front/d/9/d9662108-7a01-48d4-bb5d-edcbf1ed5a0d.jpg?1783920671",
        )
    }

    sagaChapter(2) {
        effect = Effects.Sacrifice(GameObjectFilter.Creature, 1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    sagaChapter(3) {
        effect = Effects.DrawCards(
            DynamicAmounts.countPlayersWith(
                scope = Player.EachOpponent,
                condition = Conditions.CompareAmounts(
                    left = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Creature).count(),
                    operator = ComparisonOperator.LT,
                    right = DynamicAmounts.battlefield(Player.ControllerOfSource, GameObjectFilter.Creature).count(),
                ),
            ) * 2
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "107"
        artist = "Zezhou Chen"
        imageUri = "https://cards.scryfall.io/normal/front/2/6/26386d35-7768-4b06-afe4-823028d99a48.jpg?1783920864"
    }
}
