package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Kaya, Orzhov Usurper
 * {1}{W}{B}
 * Legendary Planeswalker — Kaya
 * Loyalty 3
 *
 * +1: Exile up to two target cards from a single graveyard. You gain 2 life if at least one
 * creature card was exiled this way.
 * −1: Exile target nonland permanent with mana value 1 or less.
 * −5: Kaya deals damage to target player equal to the number of cards that player owns in exile
 * and you gain that much life.
 *
 * Modeling notes:
 *  - +1 is the Soul-Shackled Zombie shape: "from a single graveyard" is the cross-target
 *    `sameOwner` constraint, and the life gain checks the pile that was actually exiled (gathered
 *    from the chosen targets), not the possibly-fizzled targets.
 *  - −5: exile is keyed by owner, so counting the target player's exile zone is "cards that player
 *    owns in exile". The life gain reads the same count rather than the damage dealt (ruling:
 *    you gain that much even if an effect changes the damage).
 */
val KayaOrzhovUsurper = card("Kaya, Orzhov Usurper") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Planeswalker — Kaya"
    startingLoyalty = 3
    oracleText = "+1: Exile up to two target cards from a single graveyard. You gain 2 life if at " +
        "least one creature card was exiled this way.\n" +
        "−1: Exile target nonland permanent with mana value 1 or less.\n" +
        "−5: Kaya deals damage to target player equal to the number of cards that player owns in " +
        "exile and you gain that much life."

    // +1: Exile up to two target cards from a single graveyard. You gain 2 life if at least one
    //     creature card was exiled this way.
    loyaltyAbility(+1) {
        targets(TargetFilter.CardInGraveyard, count = 2, optional = true, sameOwner = true)
        effect = Effects.Pipeline {
            val kayaExiled = gather(CardSource.ChosenTargets)
            exile(kayaExiled)
            ifNotEmpty(kayaExiled, filter = GameObjectFilter.Creature) {
                run(Effects.GainLife(2))
            } orElse {
                run(Effects.Nothing)
            }
        }
        description = "Exile up to two target cards from a single graveyard. You gain 2 life if at " +
            "least one creature card was exiled this way."
    }

    // −1: Exile target nonland permanent with mana value 1 or less.
    loyaltyAbility(-1) {
        val permanent = target(TargetFilter.NonlandPermanent.manaValueAtMost(1))
        effect = Effects.Exile(permanent)
        description = "Exile target nonland permanent with mana value 1 or less."
    }

    // −5: Kaya deals damage to target player equal to the number of cards that player owns in
    //     exile and you gain that much life.
    loyaltyAbility(-5) {
        val player = target(Targets.Player)
        val exiledCount = DynamicAmounts.zone(player.asPlayer, Zone.EXILE).count()
        effect = Effects.DealDamage(exiledCount, player) then Effects.GainLife(exiledCount)
        description = "Kaya deals damage to target player equal to the number of cards that player " +
            "owns in exile and you gain that much life."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "186"
        artist = "Yongjae Choi"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fdb4b2ef-5196-4f7f-88ff-64b2cdb36c6b.jpg?1783933645"
        ruling("2019-01-25", "If a permanent has {X} in its mana cost, X is considered to be 0.")
        ruling("2019-01-25", "Tokens that aren't a copy of something else don't have a mana cost. Anything without a mana cost normally has a mana value of 0.")
        ruling("2019-01-25", "Kaya's last ability causes you to gain life equal to the number of cards the target player owns in exile, even if an effect causes her to deal more or less than that amount of damage to that player.")
    }
}
