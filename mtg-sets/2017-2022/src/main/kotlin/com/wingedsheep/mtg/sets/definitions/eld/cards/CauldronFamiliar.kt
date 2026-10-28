package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Cauldron Familiar — Throne of Eldraine #81
 * {B} · Creature — Cat · 1/1
 *
 * When this creature enters, each opponent loses 1 life and you gain 1 life.
 * Sacrifice a Food: Return this card from your graveyard to the battlefield.
 *
 * The recursion is a graveyard-zone activated ability (`activateFromZone = Zone.GRAVEYARD`, Eldrazi
 * Ravager's shape), so it can only be activated while the card is in your graveyard. The return
 * uses [Effects.PutOntoBattlefieldFromGraveyard], guarded on the card still being there on
 * resolution. The Food cost is subtype-based, so any Food artifact can pay it.
 */
val CauldronFamiliar = card("Cauldron Familiar") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Creature — Cat"
    power = 1
    toughness = 1
    oracleText = "When this creature enters, each opponent loses 1 life and you gain 1 life.\n" +
        "Sacrifice a Food: Return this card from your graveyard to the battlefield."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent)) then Effects.GainLife(1)
    }

    activatedAbility {
        cost = Costs.Sacrifice(GameObjectFilter.Any.withSubtype("Food"))
        activateFromZone = Zone.GRAVEYARD
        effect = Effects.PutOntoBattlefieldFromGraveyard(EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "81"
        artist = "Milivoj Ćeran"
        flavorText = "Every day the cat returns to kill the same mouse, which sinks again into the " +
            "cauldron's brew."
        imageUri = "https://cards.scryfall.io/normal/front/9/a/9a539a23-8383-4525-82dd-acfe1d219fe9.jpg?1783932642"
        ruling(
            "2019-10-04",
            "You can't activate Cauldron Familiar's last ability unless it's in your graveyard."
        )
        ruling(
            "2019-10-04",
            "In a Two-Headed Giant game, Cauldron Familiar's first ability causes the opposing team to " +
                "lose 1 life twice, and you gain 1 life once."
        )
    }
}
