package com.wingedsheep.mtg.sets.definitions.m12.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.AnyTarget
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Chandra, the Firebrand — Magic 2012 #124 (canonical printing; reprinted in M13)
 * {3}{R} · Legendary Planeswalker — Chandra · Loyalty 3
 *
 * +1: Chandra deals 1 damage to any target.
 * −2: When you next cast an instant or sorcery spell this turn, copy that spell. You may choose new
 *     targets for the copy.
 * −6: Chandra deals 6 damage to each of up to six targets.
 *
 * The −2 is the same [Effects.CopyNextSpellCast] rider Ral, Storm Conduit uses. The −6 picks zero to
 * six targets and deals 6 to each one still legal on resolution (Fall of the Titans' shape).
 */
val ChandraTheFirebrand = card("Chandra, the Firebrand") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Legendary Planeswalker — Chandra"
    startingLoyalty = 3
    oracleText = "+1: Chandra deals 1 damage to any target.\n" +
        "−2: When you next cast an instant or sorcery spell this turn, copy that spell. You may choose " +
        "new targets for the copy.\n" +
        "−6: Chandra deals 6 damage to each of up to six targets."

    loyaltyAbility(+1) {
        val any = target(Targets.Any)
        effect = Effects.DealDamage(1, any)
        description = "Chandra deals 1 damage to any target."
    }

    loyaltyAbility(-2) {
        effect = Effects.CopyNextSpellCast(1)
        description = "When you next cast an instant or sorcery spell this turn, copy that spell. " +
            "You may choose new targets for the copy."
    }

    loyaltyAbility(-6) {
        target = AnyTarget(count = 6, minCount = 0, optional = true)
        effect = Effects.ForEachTarget(
            Effects.DealDamage(6, EffectTarget.ContextTarget(0))
        )
        description = "Chandra deals 6 damage to each of up to six targets."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "124"
        artist = "D. Alexander Gregory"
        imageUri = "https://cards.scryfall.io/normal/front/e/f/efb37556-186f-4660-8b75-c52ef16a6d8f.jpg?1783941073"

        ruling(
            "2011-09-22",
            "Chandra's second ability creates a delayed triggered ability that will copy the next instant " +
                "or sorcery spell you cast that turn, regardless of whether that spell has targets."
        )
        ruling(
            "2011-09-22",
            "The copy is created on the stack, so it's not \"cast.\" Abilities that trigger when a player " +
                "casts a spell won't trigger."
        )
        ruling(
            "2011-09-22",
            "The copy will have the same targets as the spell it's copying unless you choose new ones. " +
                "If the copied spell is modal, the copy has the same mode; if it has an X determined as it " +
                "was cast, the copy has the same value of X."
        )
        ruling(
            "2011-09-22",
            "When you activate Chandra's last ability, you choose up to six targets. No damage is divided, " +
                "and Chandra deals 6 damage to each target that is still legal when the ability resolves."
        )
    }
}
