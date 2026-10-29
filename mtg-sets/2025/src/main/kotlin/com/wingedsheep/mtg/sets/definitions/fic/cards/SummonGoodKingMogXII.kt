package com.wingedsheep.mtg.sets.definitions.fic.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.DelayedTriggerExpiry
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Summon: Good King Mog XII — Final Fantasy Commander #26
 * {4}{W} · Enchantment Creature — Saga Moogle · 4/4
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)
 * I — Create two 1/2 white Moogle creature tokens with lifelink.
 * II, III — Whenever you cast a noncreature spell this turn, create a token that's a copy of a
 *           non-Saga token you control.
 * IV — Put two +1/+1 counters on each other Moogle you control.
 * Flying, lifelink
 *
 * - Chapters II and III set up a turn-long, every-time delayed trigger on casting a noncreature
 *   spell (Rediscover the Way's chapter III shape). It resolves before the spell that caused it,
 *   even if that spell is later countered (ruling).
 * - "A non-Saga token you control" is chosen as the delayed trigger resolves — no target — so it
 *   is [Effects.CreateTokenCopyOfChosenPermanent] (Muster the Departed) over tokens without the
 *   Saga subtype; the copy takes the token's original copiable values (CR 707.2), not its counters,
 *   tapped state or other non-copy effects. With no such token, nothing is created.
 * - Chapter IV counts every other permanent with the Moogle subtype you control as it resolves.
 */
val SummonGoodKingMogXII = card("Summon: Good King Mog XII") {
    manaCost = "{4}{W}"
    colorIdentity = "W"
    typeLine = "Enchantment Creature — Saga Moogle"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after IV.)\n" +
        "I — Create two 1/2 white Moogle creature tokens with lifelink.\n" +
        "II, III — Whenever you cast a noncreature spell this turn, create a token that's a copy of a " +
        "non-Saga token you control.\n" +
        "IV — Put two +1/+1 counters on each other Moogle you control.\n" +
        "Flying, lifelink"
    power = 4
    toughness = 4

    keywords(Keyword.FLYING, Keyword.LIFELINK)

    sagaChapter(1) {
        effect = Effects.CreateToken(
            power = 1,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Moogle"),
            keywords = setOf(Keyword.LIFELINK),
            count = 2,
            imageUri = "https://cards.scryfall.io/normal/front/2/9/295b78dc-b26d-4e92-8f75-916566c4db14.jpg?1783906134",
        )
    }

    for (chapter in 2..3) {
        sagaChapter(chapter) {
            effect = Effects.CreateDelayedTrigger(
                trigger = Triggers.you.casts(GameObjectFilter.Noncreature),
                fireOnce = false,
                expiry = DelayedTriggerExpiry.EndOfTurn,
            ) {
                effect = Effects.CreateTokenCopyOfChosenPermanent(
                    GameObjectFilter.Permanent.token().notSubtype(Subtype.SAGA).youControl()
                )
            }
        }
    }

    sagaChapter(4) {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Permanent.withSubtype("Moogle").youControl(), excludeSelf = true),
            Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 2, EffectTarget.IterationEntity),
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "26"
        artist = "Andrea Radeck"
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c14098d6-28ae-492f-8ffc-7d115d83ce6a.jpg?1783906365"
        ruling(
            "2025-06-06",
            "The delayed triggered ability set up by Summon: Good King Mog XII's second and third chapter " +
                "abilities resolves before the spell that caused it to trigger. It resolves even if that spell " +
                "is countered or otherwise leaves the stack."
        )
        ruling(
            "2025-06-06",
            "The new token copies the original characteristics of the copied token as stated by the effect " +
                "that created that token."
        )
        ruling(
            "2025-06-06",
            "The new token doesn't copy whether the original token is tapped or untapped, whether it has any " +
                "counters on it or Auras and Equipment attached to it, or any non-copy effects that have " +
                "changed its power, toughness, color, and so on."
        )
    }
}
