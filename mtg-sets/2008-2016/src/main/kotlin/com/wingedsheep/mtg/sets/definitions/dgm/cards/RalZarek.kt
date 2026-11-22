package com.wingedsheep.mtg.sets.definitions.dgm.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject
import com.wingedsheep.sdk.scripting.targets.TargetOther

/**
 * Ral Zarek
 * {2}{U}{R}
 * Legendary Planeswalker — Ral
 * Starting loyalty: 4
 *
 * +1: Tap target permanent, then untap another target permanent.
 * −2: Ral Zarek deals 3 damage to any target.
 * −7: Flip five coins. Take an extra turn after this one for each coin that comes up heads.
 *
 * The ultimate tallies heads with [Effects.FlipCoins] and repeats [Effects.TakeExtraTurn] that
 * many times, so zero heads is a clean no-op.
 */
val RalZarek = card("Ral Zarek") {
    manaCost = "{2}{U}{R}"
    colorIdentity = "UR"
    typeLine = "Legendary Planeswalker — Ral"
    startingLoyalty = 4
    oracleText = "+1: Tap target permanent, then untap another target permanent.\n" +
        "−2: Ral Zarek deals 3 damage to any target.\n" +
        "−7: Flip five coins. Take an extra turn after this one for each coin that comes up heads."

    // +1: Tap target permanent, then untap another target permanent.
    loyaltyAbility(+1) {
        val toTap = target(TargetFilter.Permanent)
        val toUntap = target(TargetOther(TargetObject(filter = TargetFilter.Permanent)))
        effect = Effects.Tap(toTap) then Effects.Untap(toUntap)
    }

    // −2: Ral Zarek deals 3 damage to any target.
    loyaltyAbility(-2) {
        val any = target(Targets.Any)
        effect = Effects.DealDamage(3, any)
    }

    // −7: Flip five coins. Take an extra turn after this one for each coin that comes up heads.
    loyaltyAbility(-7) {
        effect = Effects.Pipeline {
            val heads = runStoringNumber { Effects.FlipCoins(5, storeHeadsAs = it) }
            run(Effects.Repeat(heads.amount, Effects.TakeExtraTurn()))
        }
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "94"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/f/c/fcdbb062-0b0b-4b4c-b4db-dd149f744baa.jpg?1783940025"
        ruling("2024-01-12", "Ral Zarek's first ability can target any two permanents. It doesn't matter whether they're tapped or untapped. If the first target is tapped as the ability resolves, it will just stay tapped. Similarly, if the second target is untapped as the ability resolves, it will stay untapped.")
        ruling("2024-01-12", "The five coins you flip for Ral Zarek's last ability aren't called, and no one wins or loses these flips. Abilities that care about a player winning or losing a coin flip won't consider these flips.")
    }
}
