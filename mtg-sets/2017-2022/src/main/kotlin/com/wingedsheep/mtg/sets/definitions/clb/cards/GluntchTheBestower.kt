package com.wingedsheep.mtg.sets.definitions.clb.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.TargetPlayer

/**
 * Gluntch, the Bestower
 * {1}{G}{W}
 * Legendary Creature — Jellyfish
 * 0/5
 * Flying
 * At the beginning of your end step, choose a player. They put two +1/+1 counters on a creature
 * they control. Choose a second player to draw a card. Then choose a third player to create two
 * Treasure tokens.
 *
 * Three non-targeting `selectTarget` picks, each `excluding` the earlier ones — the rulings say
 * all three players must be different, and with only two players the third pick finds nobody, so
 * no one creates Treasure. The first player's creature is their own choice, so that leg runs in a
 * `ForEachPlayer` over the first pick, which hands them the decision.
 */
val GluntchTheBestower = card("Gluntch, the Bestower") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Legendary Creature — Jellyfish"
    power = 0
    toughness = 5
    oracleText = "Flying\n" +
        "At the beginning of your end step, choose a player. They put two +1/+1 counters on a " +
        "creature they control. Choose a second player to draw a card. Then choose a third player " +
        "to create two Treasure tokens."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.Pipeline {
            val first = selectTarget(
                TargetPlayer(descriptionOverride = "a player"),
                nonTargeting = true,
                prompt = "Choose a player to put two +1/+1 counters on a creature they control"
            )
            run(
                Effects.ForEachPlayer(
                    first.asPlayers,
                    Effects.Pipeline {
                        val creatures = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.Creature))
                        val chosen = chooseExactly(
                            1,
                            from = creatures,
                            prompt = "Choose a creature you control to put two +1/+1 counters on",
                            useTargetingUI = true
                        )
                        run(Effects.AddCountersToCollection(chosen, CounterType.PLUS_ONE_PLUS_ONE, 2))
                    }
                )
            )
            val second = selectTarget(
                TargetPlayer(descriptionOverride = "a second player"),
                nonTargeting = true,
                prompt = "Choose a second player to draw a card",
                excluding = listOf(first)
            )
            run(Effects.DrawCards(1, second.asTarget))
            val third = selectTarget(
                TargetPlayer(descriptionOverride = "a third player"),
                nonTargeting = true,
                prompt = "Choose a third player to create two Treasure tokens",
                excluding = listOf(first, second)
            )
            // Over the pick, not `controller = third`: a token's unresolved controller falls back to
            // you, and a two-player game's empty third pick must create nothing.
            run(Effects.ForEachPlayer(third.asPlayers, Effects.CreateTreasure(2)))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "275"
        artist = "Olivier Bernard"
        imageUri = "https://cards.scryfall.io/normal/front/3/b/3b3e889a-5865-4464-9923-bffa25c50cd2.jpg?1783922694"
        ruling("2022-06-10", "The first chosen player gets to choose which creature they will place the counters on.")
        ruling("2022-06-10", "You may choose a player who doesn't control any creatures as the first chosen player.")
        ruling("2022-06-10", "All three players must be different players, although any one of them can be you. If there are only two players in the game, you won't choose a third player, and no one will create Treasure tokens.")
    }
}
