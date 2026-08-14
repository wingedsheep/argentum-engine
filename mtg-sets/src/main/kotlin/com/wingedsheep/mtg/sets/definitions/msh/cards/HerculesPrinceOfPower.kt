package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Counters
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Hercules, Prince of Power — Marvel Super Heroes #171 (uncommon)
 * {2}{G} · Legendary Creature — Demigod Warrior Hero · 3/3
 *
 * Power-up — {4}{G}: Put a +1/+1 counter on Hercules. He gains vigilance, indestructible, and
 * haste until end of turn. (Activate each power-up ability only once. Reduce the cost by his mana
 * cost if he entered this turn.)
 *
 * The only power-up whose payoff is mostly *temporary*, and the reason it works: `{4}{G}` −
 * `{2}{G}` = `{2}`, so the intended line is to cast him and power up the same turn for five mana
 * total, where the haste is what makes the vigilance and indestructible matter at all. Activated
 * on a later turn at full price the three keywords still only last the turn — so the discount and
 * the temporary grants are pulling in the same direction by design.
 *
 * Each keyword is its own [Effects.GrantKeyword] at the default [com.wingedsheep.sdk.scripting.Duration.EndOfTurn];
 * only the +1/+1 counter persists.
 */
val HerculesPrinceOfPower = card("Hercules, Prince of Power") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Legendary Creature — Demigod Warrior Hero"
    oracleText = "Power-up — {4}{G}: Put a +1/+1 counter on Hercules. He gains vigilance, " +
        "indestructible, and haste until end of turn. (Activate each power-up ability only once. " +
        "Reduce the cost by his mana cost if he entered this turn.)"
    power = 3
    toughness = 3

    activatedAbility {
        isPowerUp = true
        cost = Costs.Mana("{4}{G}")
        effect = Effects.Composite(
            Effects.AddCounters(Counters.PLUS_ONE_PLUS_ONE, 1, EffectTarget.Self),
            Effects.GrantKeyword(Keyword.VIGILANCE, EffectTarget.Self),
            Effects.GrantKeyword(Keyword.INDESTRUCTIBLE, EffectTarget.Self),
            Effects.GrantKeyword(Keyword.HASTE, EffectTarget.Self)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "171"
        artist = "David Talaski"
        flavorText = "\"No need to avert your eyes, mortal admirers! Nay, I say feast them!\""
        imageUri = "https://cards.scryfall.io/normal/front/d/6/d610d9fe-f8ae-473f-9325-e2f28f7e8a69.jpg?1783902916"
    }
}
