package com.wingedsheep.mtg.sets.definitions.p02.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Goblin War Cry
 * {2}{R}
 * Sorcery
 * Target opponent chooses a creature they control. Other creatures they control can't block this turn.
 *
 * The targeted opponent picks one creature (Chooser.TargetPlayer); the remainder of their creatures,
 * snapshotted at resolution, each get a single-target CantBlock, so creatures entering later are unaffected.
 */
val GoblinWarCry = card("Goblin War Cry") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Target opponent chooses a creature they control. Other creatures they control can't block this turn."

    spell {
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            val creatures = gather(
                CardSource.ControlledPermanents(
                    player = opponent.asPlayer,
                    filter = GameObjectFilter.Creature
                )
            )
            val (_, others) = chooseExactlySplit(
                count = 1,
                from = creatures,
                chooser = Chooser.TargetPlayer,
                prompt = "Choose a creature you control. Your other creatures can't block this turn.",
                useTargetingUI = true
            )
            run(Effects.ForEachInCollection(
                collection = others,
                effect = Effects.CantBlock(EffectTarget.IterationEntity)
            ))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "104"
        artist = "Anson Maddocks"
        flavorText = "\"Goblins cry, soldiers fly.\nStones fly, soldiers die!\""
        imageUri = "https://cards.scryfall.io/normal/front/3/8/3822880c-a559-409c-8390-6d57eacc3a7c.jpg?1783946463"
    }
}
