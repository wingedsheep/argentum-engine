package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Effect
import com.wingedsheep.sdk.scripting.effects.SuccessCriterion
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Tymaret Calls the Dead
 * {2}{B}
 * Enchantment — Saga
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I, II — Mill three cards. Then you may exile a creature or enchantment card from your graveyard.
 * If you do, create a 2/2 black Zombie creature token.
 * III — You gain X life and scry X, where X is the number of Zombies you control.
 *
 * Chapters I and II share one effect (Forging the Tyrite Sword's "I, II" shape): a
 * [Patterns.Library.mill] followed by Aphemia's "you may exile a card from your graveyard. If you
 * do, …" — [Effects.May] around an [Effects.IfYouDo] whose action is a one-card gather → choose →
 * exile pipeline. The gather runs after the mill, so one of the just-milled cards may be the one
 * exiled (ruling), and exactly one card is chosen, so one Zombie at most (ruling).
 * Chapter III reads the Zombie count on resolution for both the life gain and the scry; the scry
 * runs even when X exceeds the library size (ruling).
 */
private val tymaretCallsTheDeadChapter: Effect =
    Patterns.Library.mill(3) then Effects.May(
        Effects.IfYouDo(
            action = Effects.Pipeline {
                val candidates = gather(
                    CardSource.FromZone(zone = Zone.GRAVEYARD, filter = GameObjectFilter.CreatureOrEnchantment)
                )
                // Named: the "if you do" check below reads it from outside the pipeline.
                val toExile = chooseExactly(1, from = candidates, selectedLabel = "Exile", name = "toExile")
                exile(toExile)
            },
            then = Effects.CreateToken(
                power = 2,
                toughness = 2,
                colors = setOf(Color.BLACK),
                creatureTypes = setOf("Zombie"),
                imageUri = "https://cards.scryfall.io/normal/front/6/b/6b3e6b4d-d798-4c6a-8a09-255be2a6df2c.jpg?1783931454"
            ),
            successCriterion = SuccessCriterion.CollectionNonEmpty("toExile", min = 1)
        )
    )

val TymaretCallsTheDead = card("Tymaret Calls the Dead") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I, II — Mill three cards. Then you may exile a creature or enchantment card from your graveyard. " +
        "If you do, create a 2/2 black Zombie creature token.\n" +
        "III — You gain X life and scry X, where X is the number of Zombies you control."

    sagaChapter(1) {
        effect = tymaretCallsTheDeadChapter
    }

    sagaChapter(2) {
        effect = tymaretCallsTheDeadChapter
    }

    sagaChapter(3) {
        val zombies = DynamicAmounts.battlefield(Player.You, GameObjectFilter.Permanent.withSubtype("Zombie")).count()
        effect = Effects.GainLife(zombies) then Patterns.Library.scry(zombies)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "118"
        artist = "Vincent Proce"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/68c0d087-540a-4249-9265-f13cc8e776ea.jpg?1783931559"
        ruling(
            "2020-01-24",
            "The card that you exile for the first two chapter abilities may be one of the cards that you just " +
                "put into your graveyard."
        )
        ruling(
            "2020-01-24",
            "While resolving the first two chapter abilities, you can't exile multiple cards to create " +
                "multiple Zombies."
        )
        ruling(
            "2020-01-24",
            "If X is greater than the number of cards in your library while resolving the third chapter " +
                "ability, you look at your entire library and put it back in any order. You still gain life " +
                "equal to the full value of X."
        )
    }
}
