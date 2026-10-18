package com.wingedsheep.mtg.sets.definitions.neo.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.references.Player

/**
 * The Dragon-Kami Reborn // Dragon-Kami's Egg
 * {2}{G} — Enchantment — Saga // Enchantment Creature — Egg 0/1
 *
 * Front — The Dragon-Kami Reborn:
 *   I, II — You gain 2 life. Look at the top three cards of your library. Exile one of them face
 *           down with a hatching counter on it, then put the rest on the bottom of your library in
 *           any order.
 *   III — Exile this Saga, then return it to the battlefield transformed under your control.
 *
 * Back — Dragon-Kami's Egg:
 *   Whenever this creature or a Dragon you control dies, you may cast a creature spell from among
 *   cards you own in exile with hatching counters on them without paying its mana cost.
 *
 * The hatched cards are exiled face down but stay lookable by their owner (per the ruling, even
 * after the Saga is gone). The Egg's "this creature or a Dragon you control" is two dies triggers —
 * the Egg itself isn't a Dragon, so a single Dragon filter would miss its own death — which trigger
 * exactly as the one printed ability does: once per creature that dies (the second ruling). The cast
 * happens while the trigger resolves, so creature-spell timing is ignored.
 */
private val DragonKamisEgg = card("Dragon-Kami's Egg") {
    manaCost = ""
    colorIndicator = "G"
    colorIdentity = "G"
    typeLine = "Enchantment Creature — Egg"
    oracleText = "Whenever this creature or a Dragon you control dies, you may cast a creature spell from " +
        "among cards you own in exile with hatching counters on them without paying its mana cost."
    power = 0
    toughness = 1

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = castHatchedCreature()
    }

    triggeredAbility {
        trigger = Triggers.a(GameObjectFilter.Creature.withSubtype(Subtype.DRAGON).youControl()).dies()
        effect = castHatchedCreature()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "181"
        artist = "Alix Branwyn"
        imageUri = "https://cards.scryfall.io/normal/back/2/5/2534caaa-a793-415f-977f-1d0fac3642ab.jpg?1783923856"
    }
}

/** "You may cast a creature spell from among cards you own in exile with hatching counters on them for free." */
private fun castHatchedCreature() = Effects.Pipeline {
    val hatched = gather(
        CardSource.FromZone(
            Zone.EXILE,
            Player.You,
            GameObjectFilter.Creature.withCounter(CounterType.HATCHING)
        )
    )
    val pick = chooseUpTo(1, from = hatched, selectedLabel = "Cast without paying its mana cost")
    run(Effects.CastFromCollectionWithoutPayingCost(pick))
}

/** Chapters I and II: gain 2 life, then hatch one of the top three cards face down in exile. */
private fun hatchChapter() = Effects.GainLife(2) then Effects.Pipeline {
    val top = gather(CardSource.TopOfLibrary(3))
    val split = chooseExactlySplit(
        1,
        from = top,
        selectedLabel = "Exile face down with a hatching counter",
        remainderLabel = "Put on the bottom of your library"
    )
    move(
        split.selected,
        CardDestination.ToZone(Zone.EXILE),
        faceDown = FaceDownMode.HIDDEN,
        lookableInExile = true,
        addCounterType = CounterType.HATCHING
    )
    toLibraryBottom(split.remainder)
}

private val TheDragonKamiRebornFront = card("The Dragon-Kami Reborn") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter.)\n" +
        "I, II — You gain 2 life. Look at the top three cards of your library. Exile one of them face " +
        "down with a hatching counter on it, then put the rest on the bottom of your library in any order.\n" +
        "III — Exile this Saga, then return it to the battlefield transformed under your control."

    sagaChapter(1) {
        effect = hatchChapter()
    }

    sagaChapter(2) {
        effect = hatchChapter()
    }

    sagaChapter(3) {
        effect = Effects.ExileAndReturnTransformed()
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "181"
        artist = "Alix Branwyn"
        imageUri = "https://cards.scryfall.io/normal/front/2/5/2534caaa-a793-415f-977f-1d0fac3642ab.jpg?1783923856"
        ruling(
            "2022-02-18",
            "You may look at the cards you exile with The Dragon-Kami Reborn's first two chapter abilities, " +
                "and you may continue to do so after it has left the battlefield."
        )
        ruling(
            "2022-02-18",
            "If Dragon-Kami's Egg and one or more Dragons you control die at the same time, the ability of " +
                "Dragon-Kami's Egg will trigger once for each of them. Each time one of those abilities " +
                "resolves, you'll be able to cast a creature spell from among cards you own in exile with " +
                "hatchling counters on them."
        )
    }
}

val TheDragonKamiReborn: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = TheDragonKamiRebornFront,
    backFace = DragonKamisEgg,
)
