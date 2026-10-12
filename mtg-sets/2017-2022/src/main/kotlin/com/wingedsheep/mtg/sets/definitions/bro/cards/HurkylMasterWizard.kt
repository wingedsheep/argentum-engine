package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardOrder
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator

/**
 * Hurkyl, Master Wizard
 * {1}{U}{U}
 * Legendary Creature — Human Wizard Advisor
 * 2/4
 *
 * At the beginning of your end step, if you've cast a noncreature spell this turn, reveal the top
 * five cards of your library. For each card type among noncreature spells you've cast this turn,
 * you may put a card of that type from among the revealed cards into your hand. Put the rest on
 * the bottom of your library in a random order.
 *
 * One up-to-one pick per card type a noncreature spell can have (land can't be cast, creature is
 * excluded by "noncreature"), each sized `min(1, noncreature spells of that type cast)` so a type
 * you didn't cast asks nothing. Each pick draws from the previous pick's remainder, so a revealed
 * card is chosen for at most one type (an artifact enchantment can fill the artifact slot or the
 * enchantment slot, not both).
 */
private val hurkylTypes = listOf(
    GameObjectFilter.Artifact to "artifact",
    GameObjectFilter.Battle to "battle",
    GameObjectFilter.Enchantment to "enchantment",
    GameObjectFilter.Instant to "instant",
    GameObjectFilter.Kindred to "kindred",
    GameObjectFilter.Planeswalker to "planeswalker",
    GameObjectFilter.Sorcery to "sorcery",
)

val HurkylMasterWizard = card("Hurkyl, Master Wizard") {
    manaCost = "{1}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Wizard Advisor"
    power = 2
    toughness = 4
    oracleText = "At the beginning of your end step, if you've cast a noncreature spell this turn, " +
        "reveal the top five cards of your library. For each card type among noncreature spells " +
        "you've cast this turn, you may put a card of that type from among the revealed cards into " +
        "your hand. Put the rest on the bottom of your library in a random order."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        interveningIf = Conditions.CompareAmounts(
            DynamicAmounts.spellsCastThisTurn(Player.You, GameObjectFilter.Noncreature),
            ComparisonOperator.GTE,
            1,
        )
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(DynamicAmounts.fixed(5), Player.You))
            reveal(revealed)
            var pool = revealed
            for ((type, label) in hurkylTypes) {
                val (picked, rest) = chooseUpToSplit(
                    count = DynamicAmounts.min(
                        DynamicAmounts.fixed(1),
                        DynamicAmounts.spellsCastThisTurn(Player.You, GameObjectFilter.Noncreature and type),
                    ),
                    from = pool,
                    filter = type,
                    prompt = "You may put a $label card into your hand",
                )
                toHand(picked)
                pool = rest
            }
            toLibraryBottom(pool, order = CardOrder.Random)
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "51"
        artist = "Randy Vargas"
        imageUri = "https://cards.scryfall.io/normal/front/5/f/5fcee066-425f-4341-bde5-13a5bdf06f2a.jpg"
    }
}
