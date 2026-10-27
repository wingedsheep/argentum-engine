package com.wingedsheep.mtg.sets.definitions.tdc.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.ZonePlacement
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Will of the Sultai — Tarkir: Dragonstorm Commander #49 (canonical printing)
 * {4}{G} · Sorcery
 *
 * Choose one. If you control a commander as you cast this spell, you may choose both instead.
 * • Target player mills three cards. Return all land cards from your graveyard to the battlefield
 *   tapped.
 * • Put X +1/+1 counters on target creature, where X is the number of lands you control. It gains
 *   trample until end of turn.
 *
 * The conditional mode count is the Jeska's Will shape: `minChooseCount = 1`, capped at 2 when
 * [Conditions.YouControlACommander] holds as the spell is cast. Mode one mills the target player
 * first, so lands milled from your own library come back too (Splendid Reclamation pipeline after
 * the mill). Mode two counts lands on resolution (ruling) — after mode one has returned lands when
 * both are chosen, since modes resolve in printed order.
 */
val WillOfTheSultai = card("Will of the Sultai") {
    manaCost = "{4}{G}"
    colorIdentity = "G"
    typeLine = "Sorcery"
    oracleText = "Choose one. If you control a commander as you cast this spell, you may choose both instead.\n" +
        "• Target player mills three cards. Return all land cards from your graveyard to the battlefield tapped.\n" +
        "• Put X +1/+1 counters on target creature, where X is the number of lands you control. " +
        "It gains trample until end of turn."

    spell {
        modal(
            chooseCount = 2,
            minChooseCount = 1,
            dynamicChooseCount = DynamicAmounts.conditional(
                condition = Conditions.YouControlACommander,
                ifTrue = 2,
                ifFalse = 1
            )
        ) {
            mode("Target player mills three cards. Return all land cards from your graveyard to the battlefield tapped") {
                val player = target(Targets.Player)
                effect = Patterns.Library.mill(3, player) then
                    Effects.Pipeline {
                        val graveyardLands = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Land))
                        move(graveyardLands, CardDestination.ToZone(Zone.BATTLEFIELD, placement = ZonePlacement.Tapped))
                    }
            }
            mode("Put X +1/+1 counters on target creature, where X is the number of lands you control. It gains trample until end of turn") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.AddDynamicCounters(
                    CounterType.PLUS_ONE_PLUS_ONE,
                    DynamicAmounts.landsYouControl(),
                    creature,
                ) then Effects.GrantKeyword(Keyword.TRAMPLE, creature)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "49"
        artist = "Cristi Balanescu"
        imageUri = "https://cards.scryfall.io/normal/front/3/7/37155f69-1d72-4fcb-80b3-548b7d78f9ec.jpg?1783907165"
        ruling("2025-04-04", "The value of X is calculated only once, as Will of the Sultai resolves.")
        ruling("2025-04-04", "The commander you control doesn't have to be your commander.")
        ruling(
            "2025-04-04",
            "Whether or not you control a commander is determined only once, as you choose the modes for this " +
                "spell. If you somehow lose control of that commander before you finish casting the spell " +
                "(perhaps because you sacrifice it to activate a mana ability), it won't change the number of " +
                "modes chosen."
        )
    }
}
