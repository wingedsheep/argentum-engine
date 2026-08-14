package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ReduceActivatedAbilityCost
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.scripting.values.DynamicAmount

/**
 * Hulk, Gamma Goliath — Marvel Super Heroes #215 (uncommon)
 * {3}{R}{G} · Legendary Creature — Gamma Berserker Hero · 6/5
 *
 * Reach, trample
 * Power-up abilities of other creatures you control cost {3} less to activate.
 * Power-up — {6}{R}{G}: Put five +1/+1 counters on Hulk. (Activate each power-up ability only
 * once. Reduce the cost by his mana cost if he entered this turn.)
 *
 * The cycle's payoff card, and the only one that needs `powerUpOnly` on
 * [ReduceActivatedAbilityCost] — the sibling of `exhaustOnly`. Like that flag it gates on the
 * **ability**, not the permanent: a discounted creature's ordinary activated abilities stay at
 * full price while its power-up is {3} cheaper.
 *
 * Two reductions can apply to the same ability, and they stack — CR 601.2f lets multiple cost
 * reductions be applied in any order. She-Hulk, Jade Defender's `{4}{G}{G}` becomes `{G}` the turn
 * she lands alongside Hulk: `{4}{G}{G}` − `{3}{G}` (her own mana cost) = `{1}{G}`, then − `{3}`
 * (Hulk) floors the generic at zero and leaves the colored pip. A cheaper power-up bottoms out
 * entirely: Serpent Specialist's `{3}{G}` − `{G}` = `{3}`, then − `{3}` = `{0}`, free.
 *
 * `excludeSelf` carries the printed "**other** creatures you control", so Hulk's own `{6}{R}{G}`
 * is never discounted by his static — only by his own power-up reduction, `{6}{R}{G}` −
 * `{3}{R}{G}` = `{3}`.
 */
val HulkGammaGoliath = card("Hulk, Gamma Goliath") {
    manaCost = "{3}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Gamma Berserker Hero"
    oracleText = "Reach, trample\n" +
        "Power-up abilities of other creatures you control cost {3} less to activate.\n" +
        "Power-up — {6}{R}{G}: Put five +1/+1 counters on Hulk. (Activate each power-up ability " +
        "only once. Reduce the cost by his mana cost if he entered this turn.)"
    power = 6
    toughness = 5

    keywords(Keyword.REACH, Keyword.TRAMPLE)

    staticAbility {
        ability = ReduceActivatedAbilityCost(
            filter = GroupFilter(GameObjectFilter.Creature.youControl(), excludeSelf = true),
            amount = DynamicAmount.Fixed(3),
            powerUpOnly = true
        )
    }

    activatedAbility {
        isPowerUp = true
        cost = Costs.Mana("{6}{R}{G}")
        effect = Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 5, EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "215"
        artist = "Zezhou Chen"
        imageUri = "https://cards.scryfall.io/normal/front/6/8/682b9f91-18bb-4113-9d5c-a381c191def9.jpg?1783902902"
    }
}
