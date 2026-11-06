package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ExertAsItAttacks
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Battlefield Scavenger — Amonkhet #118 (canonical printing)
 * {1}{R} · Creature — Jackal Rogue · 2/2
 *
 * You may exert this creature as it attacks.
 * Whenever you exert a creature, you may discard a card. If you do, draw a card.
 *
 * The rummage trigger watches every creature you exert, not just this one (2017-04-18 ruling).
 */
val BattlefieldScavenger = card("Battlefield Scavenger") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Jackal Rogue"
    oracleText = "You may exert this creature as it attacks. (It won't untap during your next untap step.)\n" +
        "Whenever you exert a creature, you may discard a card. If you do, draw a card."
    power = 2
    toughness = 2

    staticAbility {
        ability = ExertAsItAttacks()
    }

    triggeredAbility {
        trigger = Triggers.you.exerts(GameObjectFilter.Creature)
        effect = Effects.May(
            Effects.IfYouDo(Patterns.Hand.discardCards(1), Effects.DrawCards(1))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "118"
        artist = "Dan Murayama Scott"
        imageUri = "https://cards.scryfall.io/normal/front/f/0/f0eb1a40-8239-4544-96a1-a4acaf7e2054.jpg?1783936494"
        ruling("2017-04-18", "Some cards have abilities that trigger whenever you exert any creature. These abilities trigger when you exert that creature or any other creature you control.")
        ruling("2017-04-18", "All cards in the Amonkhet set that let you exert a creature let you do so as you declare it as an attacking creature, as do some of the cards in the Hour of Devastation set. You can't do so later in combat, and creatures put onto the battlefield attacking can't be exerted. Any abilities that trigger on exerting an attacking creature will resolve before blockers are declared.")
        ruling("2017-04-18", "If an exerted creature is already untapped during your next untap step (most likely because it had vigilance or an effect untapped it), exert's effect preventing it from untapping expires without having done anything.")
    }
}
