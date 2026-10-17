package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.mode
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Titan of Industry
 * {4}{G}{G}{G}
 * Creature — Elemental
 * 7/7
 * Reach, trample
 * When this creature enters, choose two —
 * • Destroy target artifact or enchantment.
 * • Target player gains 5 life.
 * • Create a 4/4 green Rhino Warrior creature token.
 * • Put a shield counter on a creature you control.
 *
 * The shield-counter mode doesn't target: the creature is chosen on resolution, so it can land on
 * the Rhino token made by the other mode or on the Titan itself.
 */
val TitanOfIndustry = card("Titan of Industry") {
    manaCost = "{4}{G}{G}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elemental"
    power = 7
    toughness = 7
    oracleText = "Reach, trample\n" +
        "When this creature enters, choose two —\n" +
        "• Destroy target artifact or enchantment.\n" +
        "• Target player gains 5 life.\n" +
        "• Create a 4/4 green Rhino Warrior creature token.\n" +
        "• Put a shield counter on a creature you control."

    keywords(Keyword.REACH, Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Modal(
            modes = listOf(
                mode("Destroy target artifact or enchantment.") {
                    val permanent = target(TargetFilter.ArtifactOrEnchantment)
                    effect = Effects.Destroy(permanent)
                },
                mode("Target player gains 5 life.") {
                    val player = target(Targets.Player)
                    effect = Effects.GainLife(5, player)
                },
                mode("Create a 4/4 green Rhino Warrior creature token.") {
                    effect = Effects.CreateToken(
                        power = 4,
                        toughness = 4,
                        colors = setOf(Color.GREEN),
                        creatureTypes = setOf("Rhino", "Warrior"),
                        imageUri = "https://cards.scryfall.io/normal/front/2/3/230419f9-a9f7-441d-bad7-0ed867326aa1.jpg?1783922959"
                    )
                },
                mode("Put a shield counter on a creature you control.") {
                    effect = Effects.Pipeline {
                        val creatures = gather(CardSource.ControlledPermanents(Player.You, GameObjectFilter.Creature))
                        val chosen = chooseExactly(
                            1,
                            from = creatures,
                            prompt = "Choose a creature you control to put a shield counter on",
                            useTargetingUI = true
                        )
                        run(Effects.AddCountersToCollection(chosen, CounterType.SHIELD, 1))
                    }
                }
            ),
            chooseCount = 2
        )
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "159"
        artist = "Lucas Staniec"
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b6f03c1-9e9d-4a0c-af3b-9753b440cd9f.jpg?1783923097"
    }
}
