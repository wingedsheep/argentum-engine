package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.core.AbilityFlag
import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.GrantKeywordEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Gearseeker Serpent — Kaladesh #48 (canonical; reprinted in Aetherdrift #43)
 * {5}{U}{U} · Creature — Serpent · 5/6
 *
 * Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)
 * {5}{U}: This creature can't be blocked this turn.
 *
 * Affinity is the stock [KeywordAbility.Affinity] cost reduction over [CardType.ARTIFACT] — it
 * shaves generic mana only, so the cost floors at {U}{U} no matter how many artifacts are out.
 *
 * The evasion ability grants [AbilityFlag.CANT_BE_BLOCKED] to the Serpent itself for the turn.
 * It is *not* an unblock: activating it after blockers are declared leaves the Serpent blocked
 * (per the Scryfall ruling below) — the flag only matters while blockers are being declared.
 */
val GearseekerSerpent = card("Gearseeker Serpent") {
    manaCost = "{5}{U}{U}"
    colorIdentity = "U"
    typeLine = "Creature — Serpent"
    power = 5
    toughness = 6
    oracleText = "Affinity for artifacts (This spell costs {1} less to cast for each artifact you " +
        "control.)\n" +
        "{5}{U}: This creature can't be blocked this turn."

    keywordAbility(KeywordAbility.Affinity(CardType.ARTIFACT))

    activatedAbility {
        cost = Costs.Mana("{5}{U}")
        effect = GrantKeywordEffect(AbilityFlag.CANT_BE_BLOCKED.name, EffectTarget.Self)
        description = "{5}{U}: This creature can't be blocked this turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "48"
        artist = "Filip Burburan"
        flavorText = "Its collection is vast, yet each ship has a special place in its heart."
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d32d8327-6ec2-4d43-b254-b04407612715.jpg?1783937221"
        ruling("2025-02-07", "This card's first ability can't reduce the total cost to cast the spell below {U}{U}.")
        ruling("2025-02-07", "Once this creature has been blocked, activating its last ability won't cause it to become unblocked.")
    }
}
