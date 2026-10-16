package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Vault 112: Sadistic Simulation — Fallout #123
 * {2}{U}{R} · Enchantment — Saga · Rare
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I, II — Tap up to one target creature and put a stun counter on it. You get {E}{E} (two energy
 * counters).
 * III — Pay any amount of {E}. If you paid one or more {E} this way, shuffle your library, then
 * exile that many cards from the top. You may play one of those cards without paying its mana cost.
 *
 * Chapters I and II are Frostfist Strider's tap-and-stun over an optional target, then
 * `GetEnergy(2)`; with no target chosen the energy still comes, but if the chosen creature has
 * become an illegal target the chapter doesn't resolve at all (energy ruling).
 *
 * Chapter III is Galvanic Discharge's `PayCounters(ENERGY)` — the player picks the amount, 0 up to
 * what they have — stored as "paid". Only when that's one or more does the library get shuffled and
 * "paid" cards exiled off the top. The play happens during resolution (Sunbird's Invocation's
 * choose-up-to-one shape, then Djinn of Wishes' `PlayFromCollectionWithoutPayingCost`, which casts a
 * spell for free or plays a land using a land play — only on your turn with one left). The other
 * exiled cards stay in exile.
 */
val Vault112SadisticSimulation = card("Vault 112: Sadistic Simulation") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I, II — Tap up to one target creature and put a stun counter on it. You get {E}{E} (two " +
        "energy counters).\n" +
        "III — Pay any amount of {E}. If you paid one or more {E} this way, shuffle your library, " +
        "then exile that many cards from the top. You may play one of those cards without paying " +
        "its mana cost."

    for (chapter in 1..2) {
        sagaChapter(chapter) {
            val creature = target(TargetFilter.Creature, optional = true)
            effect = Effects.Tap(creature) then
                Effects.AddCounters(CounterType.STUN, 1, creature) then
                Effects.GetEnergy(2)
        }
    }

    sagaChapter(3) {
        effect = Effects.PayCounters(CounterType.ENERGY, storeAmountAs = "paid") then
            Effects.If(
                Conditions.CompareAmounts(
                    DynamicAmounts.storedNumber("paid"),
                    ComparisonOperator.GTE,
                    1
                ),
                Effects.ShuffleLibrary() then Effects.Pipeline {
                    val exiled = gather(CardSource.TopOfLibrary(DynamicAmounts.storedNumber("paid")))
                    exile(exiled)
                    val chosen = chooseUpTo(
                        1,
                        from = exiled,
                        showAllCards = true,
                        prompt = "You may play one of the exiled cards without paying its mana cost.",
                        selectedLabel = "Play for free",
                        remainderLabel = "Leave in exile"
                    )
                    run(Effects.PlayFromCollectionWithoutPayingCost(chosen))
                }
            )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "123"
        artist = "Anton Solovianchyk"
        imageUri = "https://cards.scryfall.io/normal/front/0/a/0a0acee2-f6dc-4e92-937b-01e2c07adc55.jpg?1783912382"
    }
}
