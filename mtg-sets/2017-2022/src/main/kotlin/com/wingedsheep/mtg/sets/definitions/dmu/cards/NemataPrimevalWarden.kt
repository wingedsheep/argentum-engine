package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.RedirectZoneChangeWith
import com.wingedsheep.sdk.scripting.ReplacementRiderTiming
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Nemata, Primeval Warden — Dominaria United #209
 *
 * The death shield is Head of the Hunt's: one replacement whose "When you do, create a 1/1 green
 * Saproling creature token" is a reflexive trigger of the replacement
 * ([ReplacementRiderTiming.WHEN_YOU_DO], CR 603.12) — one Saproling trigger per creature actually
 * exiled, each going on the stack where it can be responded to.
 */
val NemataPrimevalWarden = card("Nemata, Primeval Warden") {
    manaCost = "{2}{B}{G}"
    colorIdentity = "BG"
    typeLine = "Legendary Creature — Treefolk"
    power = 3
    toughness = 4
    oracleText = "Reach\n" +
        "If a creature an opponent controls would die, exile it instead. When you do, create a 1/1 " +
        "green Saproling creature token.\n" +
        "{G}, Sacrifice a Saproling: Nemata gets +2/+2 until end of turn.\n" +
        "{1}{B}, Sacrifice two Saprolings: Draw a card."

    keywords(Keyword.REACH)

    replacementEffect(
        RedirectZoneChangeWith(
            newDestination = Zone.EXILE,
            additionalEffect = Effects.CreateToken(
                power = 1,
                toughness = 1,
                colors = setOf(Color.GREEN),
                creatureTypes = setOf("Saproling"),
            ),
            riderTiming = ReplacementRiderTiming.WHEN_YOU_DO,
            appliesTo = EventPattern.ZoneChangeEvent(
                filter = GameObjectFilter.Creature.opponentControls(),
                from = Zone.BATTLEFIELD,
                to = Zone.GRAVEYARD,
            ),
        )
    )

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{G}"),
            Costs.Sacrifice(GameObjectFilter.Permanent.withSubtype("Saproling")),
        )
        effect = Effects.ModifyStats(2, 2, EffectTarget.Self)
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{B}"),
            Costs.SacrificeMultiple(2, GameObjectFilter.Permanent.withSubtype("Saproling")),
        )
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "209"
        artist = "Andrew Mar"
        imageUri = "https://cards.scryfall.io/normal/front/a/d/adcd556d-351f-4faf-9456-0e47def69a69.jpg?1783921281"
    }
}
