package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.plus
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SearchDestination
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Enigmatic Incarnation
 * {2}{G}{U}
 * Enchantment
 * At the beginning of your end step, you may sacrifice another enchantment. If you do, search your
 * library for a creature card with mana value equal to 1 plus the sacrificed enchantment's mana
 * value, put that card onto the battlefield, then shuffle.
 *
 * Vivien on the Hunt's +2 pipeline (choose up to one to sacrifice, then an exact-MV search keyed
 * to the sacrificed permanent's last-known mana value), over *another* enchantment you control.
 * Only one enchantment can be sacrificed per resolution (ruling).
 */
val EnigmaticIncarnation = card("Enigmatic Incarnation") {
    manaCost = "{2}{G}{U}"
    colorIdentity = "GU"
    typeLine = "Enchantment"
    oracleText = "At the beginning of your end step, you may sacrifice another enchantment. If you do, " +
        "search your library for a creature card with mana value equal to 1 plus the sacrificed " +
        "enchantment's mana value, put that card onto the battlefield, then shuffle."

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.Pipeline {
            val enchantments = gather(
                CardSource.BattlefieldMatching(
                    filter = GameObjectFilter.Enchantment,
                    player = Player.You,
                    excludeSelf = true,
                )
            )
            val chosen = chooseUpTo(
                1,
                from = enchantments,
                useTargetingUI = true,
                prompt = "You may sacrifice another enchantment",
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
        description = "At the beginning of your end step, you may sacrifice another enchantment. If you do, " +
            "search your library for a creature card with mana value equal to 1 plus the sacrificed " +
            "enchantment's mana value, put that card onto the battlefield, then shuffle."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "215"
        artist = "PINDURSKI"
        imageUri = "https://cards.scryfall.io/normal/front/d/d/dd116c0b-0404-42ba-978f-7044b1a03d90.jpg?1783931521"
        ruling(
            "2020-01-24",
            "While resolving Enigmatic Incarnation's ability, you can't sacrifice multiple enchantments to get " +
                "multiple creatures."
        )
        ruling(
            "2020-01-24",
            "If an enchantment on the battlefield or a creature card in a library has {X} in its mana cost, X is " +
                "considered to be 0."
        )
    }
}
