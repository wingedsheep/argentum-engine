package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.Duration
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChange
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cry of the Carnarium — Ravnica Allegiance #70
 * {1}{B}{B} · Sorcery
 *
 * All creatures get -2/-2 until end of turn. Exile all creature cards in all graveyards that were
 * put there from the battlefield this turn. If a creature would die this turn, exile it instead.
 *
 * Malicious Malfunction's shape plus Second Sunrise's graveyard sweep:
 *  - the death replacement is a floating, caster-anchored `GrantReplacementEffect(RedirectZoneChange)`
 *    over every creature, so it also covers creatures that weren't on the battlefield as the spell
 *    resolved (ruling) — not just the ones it shrank;
 *  - the sweep gathers creature cards from every graveyard filtered by
 *    `putIntoGraveyardFromBattlefieldThisTurn()`. Creatures killed by the -2/-2 are still on the
 *    battlefield during resolution (state-based actions wait), so they aren't swept — they die
 *    afterwards and the replacement exiles them instead.
 */
val CryOfTheCarnarium = card("Cry of the Carnarium") {
    manaCost = "{1}{B}{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "All creatures get -2/-2 until end of turn. Exile all creature cards in all graveyards " +
        "that were put there from the battlefield this turn. If a creature would die this turn, exile it instead."

    spell {
        effect = Effects.ForEachInGroup(
            filter = GroupFilter.AllCreatures,
            effect = Effects.ModifyStats(-2, -2, EffectTarget.IterationEntity)
        ) then Effects.Pipeline {
            val diedThisTurn = gather(
                CardSource.FromZone(
                    zone = Zone.GRAVEYARD,
                    player = Player.Each,
                    filter = GameObjectFilter.Creature.putIntoGraveyardFromBattlefieldThisTurn()
                )
            )
            move(diedThisTurn, CardDestination.ToZone(Zone.EXILE))
        } then Effects.GrantReplacementEffect(
            replacement = RedirectZoneChange(
                newDestination = Zone.EXILE,
                appliesTo = EventPattern.ZoneChangeEvent(
                    filter = GameObjectFilter.Creature,
                    from = Zone.BATTLEFIELD,
                    to = Zone.GRAVEYARD
                )
            ),
            target = EffectTarget.Self,
            duration = Duration.EndOfTurn
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "70"
        artist = "Johann Bodin"
        flavorText = "If your ears bleed, it's a party. If your eyes bleed, it's a Rakdos party."
        imageUri = "https://cards.scryfall.io/normal/front/7/1/715a14a3-046e-45ca-b943-dd630e5202b7.jpg?1783933695"
        ruling(
            "2019-01-25",
            "Cry of the Carnarium's power-and-toughness-reducing effect affects only creatures on the " +
                "battlefield at the time it resolves. Creatures that enter the battlefield later in the turn won't get -2/-2."
        )
        ruling(
            "2019-01-25",
            "Cry of the Carnarium's replacement effect affects any creature that would die in the turn, " +
                "even creatures not on the battlefield as it resolves."
        )
        ruling(
            "2019-01-25",
            "Creatures that would die after getting -2/-2 will still be on the battlefield when cards are " +
                "exiled from graveyards. They won't die, and will instead be exiled after Cry of the Carnarium " +
                "is entirely finished resolving."
        )
    }
}
