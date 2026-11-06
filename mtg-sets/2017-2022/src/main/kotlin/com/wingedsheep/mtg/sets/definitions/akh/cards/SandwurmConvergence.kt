package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantAttackYou
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Sandwurm Convergence
 * {6}{G}
 * Enchantment
 * Creatures with flying can't attack you or planeswalkers you control.
 * At the beginning of your end step, create a 5/5 green Wurm creature token.
 *
 * The restriction is checked only as attackers are declared, so a creature already attacking that
 * gains flying stays in combat (per the 2017-04-18 ruling).
 */
val SandwurmConvergence = card("Sandwurm Convergence") {
    manaCost = "{6}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment"
    oracleText = "Creatures with flying can't attack you or planeswalkers you control.\n" +
        "At the beginning of your end step, create a 5/5 green Wurm creature token."

    staticAbility {
        ability = CantAttackYou(
            GroupFilter(GameObjectFilter.Creature.withKeyword(Keyword.FLYING)),
            coversPlaneswalkers = true
        )
    }

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        effect = Effects.CreateToken(
            power = 5,
            toughness = 5,
            colors = setOf(Color.GREEN),
            creatureTypes = setOf("Wurm"),
            imageUri = "https://cards.scryfall.io/normal/front/3/6/366c01a7-6399-496b-a5a3-8be45475ce33.jpg?1783936409"
        )
        description = "At the beginning of your end step, create a 5/5 green Wurm creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "183"
        artist = "Slawomir Maniak"
        flavorText = "Cantankerous and territorial, sandwurms claim even the skies above their dunes."
        imageUri = "https://cards.scryfall.io/normal/front/e/4/e47bd2c5-e3db-4fde-a695-9e67a7e937be.jpg?1783936471"

        ruling(
            "2017-04-18",
            "Once a creature has attacked you or a planeswalker you control, it will remain in combat " +
                "even if it gains flying. Similarly, if a creature with flying enters the battlefield " +
                "attacking, Sandwurm Convergence's first ability can't undo that attack."
        )
    }
}
