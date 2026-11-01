package com.wingedsheep.mtg.sets.definitions.war.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetPermanentOrPlayer

/**
 * Price of Betrayal
 * {B}
 * Sorcery
 * Remove up to five counters from target artifact, creature, planeswalker, or opponent.
 *
 * One target — an artifact, creature or planeswalker on the battlefield, or an opponent
 * ([TargetPermanentOrPlayer] with `opponentsOnly`). [Effects.RemoveCountersUpTo] is the budget-capped
 * removal: the caster picks how many of each kind come off that one target, five in total, and may
 * remove none. Player counters (poison, energy, experience) live on the player entity, so the same
 * walk covers an opponent.
 */
val PriceOfBetrayal = card("Price of Betrayal") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Sorcery"
    oracleText = "Remove up to five counters from target artifact, creature, planeswalker, or opponent."

    spell {
        val victim = target(
            TargetPermanentOrPlayer(
                permanentFilter = TargetFilter(
                    GameObjectFilter.Artifact or GameObjectFilter.Creature or GameObjectFilter.Planeswalker
                ),
                opponentsOnly = true,
                descriptionOverride = "target artifact, creature, planeswalker, or opponent"
            )
        )
        effect = Effects.RemoveCountersUpTo(5, victim)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "102"
        artist = "Ryan Yee"
        flavorText = "The moment Liliana defied Nicol Bolas, her contract was broken, and her life was forfeit. " +
            "But she was free to choose her fate, and she decided it was worth the price."
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd68e918-5089-46c9-8836-b05a3da3c7a3.jpg?1783933440"
        ruling(
            "2019-05-03",
            "Price of Betrayal has only one target. You may remove different kinds of counters from that one " +
                "target, and you may also remove multiples of the same kind of counter from that one target."
        )
        ruling(
            "2019-05-03",
            "Players don't normally have counters, but some cards in other sets say that a player \"gets\" a " +
                "counter of a certain kind. Notably, emblems aren't counters."
        )
    }
}
