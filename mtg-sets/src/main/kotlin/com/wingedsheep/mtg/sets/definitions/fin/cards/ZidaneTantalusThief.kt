package com.wingedsheep.mtg.sets.definitions.fin.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration

/**
 * Zidane, Tantalus Thief
 * {3}{R}{W}
 * Legendary Creature — Human Mutant Scout
 * 3/3
 *
 * When Zidane enters, gain control of target creature an opponent controls until end of turn.
 * Untap it. It gains lifelink and haste until end of turn.
 * Whenever an opponent gains control of a permanent from you, you create a Treasure token.
 *
 * The second ability uses [Triggers.OpponentGainsControlOfYourPermanent] — a resident, battlefield-
 * wide control-change watcher that fires once for each permanent an opponent takes from you. Per the
 * official ruling it fires for each such permanent (creating one Treasure each), including Zidane
 * itself when it is the permanent being stolen (the trigger still resolves for its old controller).
 */
val ZidaneTantalusThief = card("Zidane, Tantalus Thief") {
    manaCost = "{3}{R}{W}"
    colorIdentity = "WR"
    typeLine = "Legendary Creature — Human Mutant Scout"
    oracleText = "When Zidane enters, gain control of target creature an opponent controls until end of turn. Untap it. It gains lifelink and haste until end of turn.\nWhenever an opponent gains control of a permanent from you, you create a Treasure token."
    power = 3
    toughness = 3
    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        val t = target("target creature an opponent controls", Targets.CreatureOpponentControls)
        effect = Effects.Composite(
            Effects.GainControl(t, Duration.EndOfTurn),
            Effects.Untap(t),
            Effects.GrantKeyword(Keyword.LIFELINK, t),
            Effects.GrantKeyword(Keyword.HASTE, t)
        )
    }
    triggeredAbility {
        trigger = Triggers.OpponentGainsControlOfYourPermanent
        effect = Effects.CreateTreasure(1)
    }
    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "251"
        artist = "Eiji Kaneda"
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e42c7d9d-8685-415b-8c5d-6ab2165863b9.jpg?1782686402"
        ruling("2025-06-06", "If an opponent gains control of Zidane from you, Zidane's last ability will trigger and you'll create a Treasure token. If an opponent gains control of Zidane and one or more other permanents from you simultaneously, Zidane's last ability will trigger for each of those permanents, including itself. This means you'll create a Treasure token for each of those permanents, including Zidane.")
    }
}
