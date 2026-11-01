package com.wingedsheep.mtg.sets.definitions.rna.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Bolrac-Clan Crusher
 * {3}{R}{G}
 * Creature — Ogre Warrior
 * 4/4
 *
 * {T}, Remove a +1/+1 counter from a creature you control: This creature deals 2 damage to any target.
 */
val BolracClanCrusher = card("Bolrac-Clan Crusher") {
    manaCost = "{3}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Creature — Ogre Warrior"
    power = 4
    toughness = 4
    oracleText = "{T}, Remove a +1/+1 counter from a creature you control: This creature deals 2 damage to any target."

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.RemovePlusOnePlusOneCounters(GameObjectFilter.Creature.youControl(), 1)
        )
        val t = target(Targets.Any)
        effect = Effects.DealDamage(2, t)
        description = "This creature deals 2 damage to any target."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "159"
        artist = "Craig J Spearing"
        flavorText = "Jaharg discovered that the shapes and symmetries so pleasing to the sculptor's eye " +
            "also yielded a well-balanced bludgeon."
        imageUri = "https://cards.scryfall.io/normal/front/5/0/5080975e-693a-44f5-a718-9ee947dada6d.jpg?1783933657"

        ruling(
            "2019-01-25",
            "You must remove exactly one +1/+1 counter from a creature you control to activate " +
                "Bolrac-Clan Crusher's ability; you can't activate it without removing a +1/+1 counter, " +
                "and you can't remove additional counters."
        )
    }
}
