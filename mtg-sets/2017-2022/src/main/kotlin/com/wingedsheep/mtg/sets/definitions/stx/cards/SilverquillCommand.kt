package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Silverquill Command — Strixhaven: School of Mages #232 (canonical printing)
 * {2}{W}{B} · Sorcery
 *
 * Choose two —
 * • Target creature gets +3/+3 and gains flying until end of turn.
 * • Return target creature card with mana value 2 or less from your graveyard to the battlefield.
 * • Target player draws a card and loses 1 life.
 * • Target opponent sacrifices a creature of their choice.
 *
 * A `modal(chooseCount = 2)` spell with per-mode targets, like the other Strixhaven Commands. The
 * sacrifice mode is Pharika's Libation's [Effects.Sacrifice] over the targeted opponent.
 */
val SilverquillCommand = card("Silverquill Command") {
    manaCost = "{2}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Sorcery"
    oracleText =
        "Choose two —\n" +
        "• Target creature gets +3/+3 and gains flying until end of turn.\n" +
        "• Return target creature card with mana value 2 or less from your graveyard to the battlefield.\n" +
        "• Target player draws a card and loses 1 life.\n" +
        "• Target opponent sacrifices a creature of their choice."

    spell {
        modal(chooseCount = 2) {
            mode("Target creature gets +3/+3 and gains flying until end of turn") {
                val creature = target(TargetFilter.Creature)
                effect = Effects.ModifyStats(3, 3, creature) then
                    Effects.GrantKeyword(Keyword.FLYING, creature)
            }
            mode("Return target creature card with mana value 2 or less from your graveyard to the battlefield") {
                val creatureCard = target(TargetFilter.CreatureInYourGraveyard.manaValueAtMost(2))
                effect = Effects.PutOntoBattlefieldFromGraveyard(creatureCard)
            }
            mode("Target player draws a card and loses 1 life") {
                val player = target(Targets.Player)
                effect = Effects.DrawCards(1, player) then Effects.LoseLife(1, player)
            }
            mode("Target opponent sacrifices a creature of their choice") {
                val opponent = target(Targets.Opponent)
                effect = Effects.Sacrifice(filter = GameObjectFilter.Creature, target = opponent)
            }
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "232"
        artist = "Bryan Sola"
        imageUri = "https://cards.scryfall.io/normal/front/5/c/5c55f533-9d31-4ad1-b336-927aba6e74d6.jpg?1783927293"
        ruling("2021-04-16", "If all of Silverquill Command's targets are illegal as it tries to resolve, it will do nothing.")
        ruling("2021-04-16", "If at least one target is still legal as Silverquill Command tries to resolve, it will resolve and do as much as it can.")
    }
}
