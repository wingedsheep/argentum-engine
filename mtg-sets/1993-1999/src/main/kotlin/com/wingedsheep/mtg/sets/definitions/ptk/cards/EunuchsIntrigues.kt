package com.wingedsheep.mtg.sets.definitions.ptk.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Eunuchs' Intrigues
 * {2}{R}
 * Sorcery
 * Target opponent chooses a creature they control. Other creatures they control can't block this turn.
 *
 * The targeted opponent picks one creature (Chooser.TargetPlayer); the remainder of their creatures,
 * snapshotted at resolution, each get a single-target CantBlock, so creatures entering later are unaffected.
 */
val EunuchsIntrigues = card("Eunuchs' Intrigues") {
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
        collectorNumber = "110"
        artist = "Li Yousong"
        flavorText = "Taking control of powerful court positions one by one, eunuchs eventually brought on the fall of the Han dynasty, leading to the chaos of the Three Kingdoms."
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5e7ca92f-770d-4147-8eaf-f1fa69340dc9.jpg?1783946107"
    }
}
