package com.wingedsheep.mtg.sets.definitions.snc.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Vivien on the Hunt
 * {4}{G}{G}
 * Legendary Planeswalker — Vivien
 * Loyalty 4
 * +2: You may sacrifice a creature. If you do, search your library for a creature card with mana
 * value equal to 1 plus the sacrificed creature's mana value, put it onto the battlefield, then
 * shuffle.
 * +1: Mill five cards, then put any number of creature cards milled this way into your hand.
 * −1: Create a 4/4 green Rhino Warrior creature token.
 *
 * Modeling notes:
 *  - The +2's sacrifice is a resolution-time choice, not a cost (the Kethek / Birthing Ritual
 *    idiom): offer up to one creature you control, and gate the search on one being chosen.
 *    Declining does nothing at all — no search, no shuffle (2022-04-29 ruling).
 *  - The search's filter reads the sacrificed creature's mana value as it last existed through
 *    [EffectTarget.SacrificedAsCost] (the pipeline's sacrifice records the snapshot), fed into the
 *    exact-equality [GameObjectFilter.manaValueEqualsDynamic] predicate — Sidisi's "X plus 1".
 */
val VivienOnTheHunt = card("Vivien on the Hunt") {
    manaCost = "{4}{G}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Planeswalker — Vivien"
    startingLoyalty = 4
    oracleText = "+2: You may sacrifice a creature. If you do, search your library for a creature " +
        "card with mana value equal to 1 plus the sacrificed creature's mana value, put it onto " +
        "the battlefield, then shuffle.\n" +
        "+1: Mill five cards, then put any number of creature cards milled this way into your hand.\n" +
        "−1: Create a 4/4 green Rhino Warrior creature token."

    loyaltyAbility(+2) {
        effect = Effects.Pipeline {
            val creatures = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Creature,
                    player = Player.You,
                )
            )
            val chosen = chooseUpTo(
                1,
                from = creatures,
                useTargetingUI = true,
                prompt = "You may sacrifice a creature",
                selectedLabel = "Sacrifice",
            )
            ifNotEmpty(chosen) {
                sacrifice(chosen)
                run(
                    Patterns.Library.searchLibrary(
                        filter = GameObjectFilter.Creature.manaValueEqualsDynamic(
                            DynamicAmounts.manaValueOf(EffectTarget.SacrificedAsCost(0)) + 1
                        ),
                        count = 1,
                        destination = SearchDestination.BATTLEFIELD,
                        shuffleAfter = true,
                    )
                )
            }
        }
        description = "You may sacrifice a creature. If you do, search your library for a creature " +
            "card with mana value equal to 1 plus the sacrificed creature's mana value, put it onto " +
            "the battlefield, then shuffle."
    }

    loyaltyAbility(+1) {
        effect = Effects.Pipeline {
            val milled = mill(5)
            val creatures = filter(milled, GameObjectFilter.Creature)
            val chosen = chooseAnyNumber(
                from = creatures,
                prompt = "Put any number of creature cards milled this way into your hand",
                selectedLabel = "Put into your hand",
            )
            toHand(chosen)
        }
        description = "Mill five cards, then put any number of creature cards milled this way into " +
            "your hand."
    }

    loyaltyAbility(-1) {
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Rhino", "Warrior"),
            imageUri = "https://cards.scryfall.io/normal/front/2/3/230419f9-a9f7-441d-bad7-0ed867326aa1.jpg?1783922959",
        )
        description = "Create a 4/4 green Rhino Warrior creature token."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "162"
        artist = "Jake Murray"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5edfb119-abc9-4d4d-bc46-33cce1a0922d.jpg?1783923096"
        ruling("2022-04-29", "You may activate Vivien on the Hunt's first ability without sacrificing a creature. If you do, the ability does nothing.")
    }
}
