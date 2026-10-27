package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.SelectionRestriction
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Battle for Bretagard — Kaldheim #203 (canonical printing)
 * {1}{G}{W} · Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Create a 1/1 white Human Warrior creature token.
 * II — Create a 1/1 green Elf Warrior creature token.
 * III — Choose any number of artifact tokens and/or creature tokens you control with different
 * names. For each of them, create a token that's a copy of it.
 *
 * Chapter III doesn't target (ruling): the artifact/creature tokens you control are gathered on
 * resolution, you pick any number held to different names by [SelectionRestriction.OnePerCardName],
 * and each picked token is copied once. A Human Warrior Token and an Elf Warrior Token have
 * different names, so both may be copied.
 */
val BattleForBretagard = card("Battle for Bretagard") {
    manaCost = "{1}{G}{W}"
    colorIdentity = "GW"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Create a 1/1 white Human Warrior creature token.\n" +
        "II — Create a 1/1 green Elf Warrior creature token.\n" +
        "III — Choose any number of artifact tokens and/or creature tokens you control with different " +
        "names. For each of them, create a token that's a copy of it."

    sagaChapter(1) {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Human", "Warrior")
        )
    }

    sagaChapter(2) {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 1,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Elf", "Warrior")
        )
    }

    sagaChapter(3) {
        effect = Effects.Pipeline {
            val tokens = gather(
                CardSource.ControlledPermanents(Player.You, GameObjectFilter.CreatureOrArtifact.token())
            )
            val chosen = chooseAnyNumber(
                from = tokens,
                useTargetingUI = true,
                restrictions = listOf(SelectionRestriction.OnePerCardName),
                prompt = "Choose any number of artifact and/or creature tokens you control with " +
                    "different names to copy"
            )
            run(Effects.ForEachInCollection(chosen, Effects.CreateTokenCopyOfTarget(EffectTarget.IterationEntity)))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "203"
        artist = "Igor Kieryluk"
        imageUri = "https://cards.scryfall.io/normal/front/c/d/cd31ec15-2fe0-40ab-b320-bc4333db4787.jpg?1783928201"
        ruling("2021-02-05", "The chapter III ability doesn't target any of the tokens. You choose which ones you're copying as the ability resolves.")
        ruling("2021-02-05", "Unless the effect that creates a token specifies a different name, the name of a token is the same as its creature types plus the word \"Token\". In particular, a Human Warrior creature token has a different name than an Elf Warrior creature token, and you may create a copy of each using the chapter III ability.")
        ruling("2021-02-05", "Each token you create copies the original characteristics of the token it's copying as stated by the effect that created that token. The newly created token doesn't copy whether the original token is tapped or untapped, whether it has any counters on it, whether it has any Auras and/or Equipment attached to it, or any non-copy effects that changed its power, toughness, types, color, and so on.")
    }
}
