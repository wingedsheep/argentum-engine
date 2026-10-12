package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBlock
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Razorlash Transmogrant — The Brothers' War #122
 * {2} · Artifact Creature — Zombie · 3/1 · Rare
 *
 * This creature can't block.
 * {4}{B}{B}: Return this card from your graveyard to the battlefield with a +1/+1 counter on it.
 * This ability costs {4} less to activate if an opponent controls four or more nonbasic lands.
 *
 * The recursion is the graveyard-activation shape (Firemane Angel): `activateFromZone =
 * GRAVEYARD` and a `fromZone = GRAVEYARD` move, with `addCounterType` so the card *enters* with
 * the counter rather than getting it afterwards. The discount is `costsLessIf` (Kami of Jealous
 * Thirst); "an opponent controls four or more" is existential per opponent — the greatest
 * nonbasic-land count among your opponents is at least four — so two opponents with two each in
 * multiplayer do not satisfy it.
 */
val RazorlashTransmogrant = card("Razorlash Transmogrant") {
    manaCost = "{2}"
    colorIdentity = "B"
    typeLine = "Artifact Creature — Zombie"
    power = 3
    toughness = 1
    oracleText = "This creature can't block.\n" +
        "{4}{B}{B}: Return this card from your graveyard to the battlefield with a +1/+1 counter " +
        "on it. This ability costs {4} less to activate if an opponent controls four or more " +
        "nonbasic lands."

    staticAbility {
        ability = CantBlock(filter = GroupFilter.source())
    }

    activatedAbility {
        cost = Costs.Mana("{4}{B}{B}")
        activateFromZone = Zone.GRAVEYARD
        costsLessIf(
            "{4}",
            Conditions.CompareAmounts(
                DynamicAmounts.greatestAmongPlayers(
                    DynamicAmounts.battlefield(Player.You, GameObjectFilter.NonbasicLand).count(),
                    players = Player.EachOpponent
                ),
                ComparisonOperator.GTE,
                4
            )
        )
        effect = Effects.Move(
            EffectTarget.Self,
            Zone.BATTLEFIELD,
            fromZone = Zone.GRAVEYARD,
            addCounterType = CounterType.PLUS_ONE_PLUS_ONE
        )
        description = "{4}{B}{B}: Return this card from your graveyard to the battlefield with a " +
            "+1/+1 counter on it. This ability costs {4} less to activate if an opponent controls " +
            "four or more nonbasic lands."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "122"
        artist = "Kekai Kotaki"
        imageUri = "https://cards.scryfall.io/normal/front/e/d/edfaf8cc-7819-4ec2-8d79-e3b0c1814590.jpg?1783920075"
    }
}
