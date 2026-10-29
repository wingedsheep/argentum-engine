package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nicol Bolas, God-Pharaoh — Hour of Devastation #140
 * {4}{U}{B}{R} · Legendary Planeswalker — Bolas · Loyalty 7
 *
 * +2: Target opponent exiles cards from the top of their library until they exile a nonland card.
 *     Until end of turn, you may cast that card without paying its mana cost.
 * +1: Each opponent exiles two cards from their hand.
 * −4: Nicol Bolas deals 7 damage to target opponent, creature an opponent controls, or
 *     planeswalker an opponent controls.
 * −12: Exile each nonland permanent your opponents control.
 *
 * +2 is Codie's "exile until … then cast it free this turn" pipeline, walked over the target
 * opponent's library (`gatherUntilMatch(Nonland, Player.TargetOpponent)`, Sméagol's shape); the
 * grant outlives Bolas (ruling) and an uncast card stays exiled. +1 is
 * `EachOpponentExilesFromHand(2)` — each opponent picks, and one with two or fewer cards exiles
 * their whole hand (ruling). −4 is All Will Be One's target. −12 is Perilous Vault's group exile
 * narrowed to opponents' nonland permanents.
 */
val NicolBolasGodPharaoh = card("Nicol Bolas, God-Pharaoh") {
    manaCost = "{4}{U}{B}{R}"
    colorIdentity = "UBR"
    typeLine = "Legendary Planeswalker — Bolas"
    startingLoyalty = 7
    oracleText = "+2: Target opponent exiles cards from the top of their library until they exile a nonland " +
        "card. Until end of turn, you may cast that card without paying its mana cost.\n" +
        "+1: Each opponent exiles two cards from their hand.\n" +
        "−4: Nicol Bolas deals 7 damage to target opponent, creature an opponent controls, or planeswalker " +
        "an opponent controls.\n" +
        "−12: Exile each nonland permanent your opponents control."

    loyaltyAbility(+2) {
        target(Targets.Opponent)
        effect = Effects.Pipeline {
            val (found, revealed) = gatherUntilMatch(GameObjectFilter.Nonland, player = Player.TargetOpponent)
            exile(revealed)
            run(Effects.GrantMayPlayFromExile(found, expiry = MayPlayExpiry.EndOfTurn))
            run(Effects.GrantPlayWithoutPayingCost(found, expiry = MayPlayExpiry.EndOfTurn))
        }
        description = "Target opponent exiles cards from the top of their library until they exile a nonland " +
            "card. Until end of turn, you may cast that card without paying its mana cost."
    }

    loyaltyAbility(+1) {
        effect = Effects.EachOpponentExilesFromHand(2)
        description = "Each opponent exiles two cards from their hand."
    }

    loyaltyAbility(-4) {
        val t = target(Targets.OpponentOrTheirCreatureOrPlaneswalker)
        effect = Effects.DealDamage(7, t)
        description = "Nicol Bolas deals 7 damage to target opponent, creature an opponent controls, or " +
            "planeswalker an opponent controls."
    }

    loyaltyAbility(-12) {
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.NonlandPermanent.opponentControls()),
            Effects.Exile(EffectTarget.IterationEntity)
        )
        description = "Exile each nonland permanent your opponents control."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "140"
        artist = "Raymond Swanland"
        imageUri = "https://cards.scryfall.io/normal/front/9/8/985e92f4-3904-41df-91f7-42eedbe3ccd2.jpg?1783936012"
        ruling("2017-07-14", "The cards exiled by Nicol Bolas's first and second abilities are exiled face up.")
        ruling(
            "2017-07-14",
            "You may cast the nonland card exiled by Nicol Bolas's first ability that turn even if Nicol Bolas is " +
                "no longer on the battlefield or under your control."
        )
        ruling(
            "2017-07-14",
            "Casting the card exiled with Nicol Bolas's first ability follows the normal timing rules for casting " +
                "that card."
        )
        ruling("2017-07-14", "If you don't cast the card exiled by Nicol Bolas's first ability that turn, it will remain exiled.")
        ruling(
            "2017-07-14",
            "While resolving Nicol Bolas's second ability, each opponent chooses which cards to exile from their " +
                "hand. Any opponent with two or fewer cards exiles their entire hand."
        )
    }
}
