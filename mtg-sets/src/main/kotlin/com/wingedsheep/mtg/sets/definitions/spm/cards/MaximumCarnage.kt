package com.wingedsheep.mtg.sets.definitions.spm.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Maximum Carnage (SPM #83)
 * {4}{R} — Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Until your next turn, each creature attacks each combat if able and attacks a player
 *     other than you if able.
 * II — Add {R}{R}{R}.
 * III — This Saga deals 5 damage to each opponent.
 *
 * Chapter I is a mass goad (CR 701.15): goad's designation is precisely "attacks each combat if
 * able and attacks a player other than the controller of the ability that goaded it, until that
 * controller's next turn" (701.15a–b) — word-for-word chapter I, with "you" being the Saga's
 * controller. We apply it to every creature via [Effects.ForEachInGroup] over
 * [GroupFilter.AllCreatures] (every creature on the battlefield, your own included), running
 * [Effects.Goad] on each iteration entity ([EffectTarget.Self]). Group iteration does NOT rebind
 * the controller (only per-player iteration does), so the goader of record stays the Saga's
 * controller = "you" for every creature, and the built-in until-your-next-turn expiry (701.15a)
 * supplies the "Until your next turn" duration.
 *
 * Chapter II adds {R}{R}{R} to the controller's mana pool ([Effects.AddMana]).
 *
 * Chapter III deals 5 damage to each opponent, source defaulting to the Saga ("This Saga deals
 * 5 damage") ([Effects.DealDamage] to [Player.EachOpponent]).
 */
val MaximumCarnage = card("Maximum Carnage") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Until your next turn, each creature attacks each combat if able and attacks a player other than you if able.\n" +
        "II — Add {R}{R}{R}.\n" +
        "III — This Saga deals 5 damage to each opponent."

    // I — Until your next turn, each creature attacks each combat if able and attacks a player
    //     other than you if able. (Mass goad by you — CR 701.15.)
    sagaChapter(1) {
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreatures,
            effect = Effects.Goad(EffectTarget.Self)
        )
    }

    // II — Add {R}{R}{R}.
    sagaChapter(2) {
        effect = Effects.AddMana(Color.RED, 3)
    }

    // III — This Saga deals 5 damage to each opponent.
    sagaChapter(3) {
        effect = Effects.DealDamage(5, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "83"
        artist = "Bill Sienkiewicz"
        imageUri = "https://cards.scryfall.io/normal/front/7/d/7d72d867-6ed2-4900-a8ae-9d86f581ce32.jpg?1783905335"
    }
}
