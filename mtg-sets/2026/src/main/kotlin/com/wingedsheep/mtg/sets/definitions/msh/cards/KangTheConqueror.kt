package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Kang the Conqueror — Marvel Super Heroes #62 (mythic)
 * {2}{U}{U} · Legendary Creature — Human Villain · 4/5
 *
 * Flying
 * Power-up — {5}{U}{U}{U}: Put a +1/+1 counter on Kang. Take an extra turn after this one. During
 * that turn, power-up abilities can't be activated. (Activate each power-up ability only once.
 * Reduce the cost by his mana cost if he entered this turn.)
 *
 * Everything but the last clause is stock: `isPowerUp` carries the once-per-object limit and the
 * pip-wise self-cost reduction (`{5}{U}{U}{U}` − `{2}{U}{U}` = `{3}{U}` the turn he lands), and the
 * counter plus the extra turn are `Effects.AddCounters` and `Effects.TakeExtraTurn`.
 *
 * The lockout is the `powerUpAbilitiesCantBeActivated` rider on [Effects.TakeExtraTurn] rather than
 * a separately sequenced effect, because "that turn" is the turn *that* effect creates. The engine
 * records the rider on the pending extra turn and applies it when that turn begins; a
 * `SkipExtraTurns` source (Ugin's Nexus, Trouble in Pairs) that skips the turn as it would begin
 * takes the prohibition with it, since there is no "that turn" for it to bind to. A sibling in the
 * `Composite` would have to re-derive that.
 *
 * The prohibition covers **every** player's power-up abilities, not just Kang's controller's, and
 * it outlives Kang: killing him in response to the ability, or during the extra turn, does not lift
 * it. The engine therefore records the extra turn's number, once it begins, in `GameState.powerUpRestrictedTurns`
 * instead of hanging a static ability off the permanent.
 */
val KangTheConqueror = card("Kang the Conqueror") {
    manaCost = "{2}{U}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Human Villain"
    oracleText = "Flying\n" +
        "Power-up — {5}{U}{U}{U}: Put a +1/+1 counter on Kang. Take an extra turn after this one. " +
        "During that turn, power-up abilities can't be activated. (Activate each power-up ability " +
        "only once. Reduce the cost by his mana cost if he entered this turn.)"
    power = 4
    toughness = 5

    keywords(Keyword.FLYING)

    activatedAbility {
        isPowerUp = true
        cost = Costs.Mana("{5}{U}{U}{U}")
        effect = Effects.AddCounters(CounterType.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self) then
            Effects.TakeExtraTurn(powerUpAbilitiesCantBeActivated = true)
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "62"
        artist = "Peter Scanlan"
        flavorText = "\"My time . . . is all times.\""
        imageUri = "https://cards.scryfall.io/normal/front/7/9/79747b45-fd7b-4023-9a9c-4d9dab2429fe.jpg?1783902956"
    }
}
