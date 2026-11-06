package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ExertAsItAttacks
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Champion of Rhonas — Amonkhet #159 (canonical printing)
 * {3}{G} · Creature — Jackal Warrior · 3/3
 *
 * You may exert this creature as it attacks. When you do, you may put a creature card from your
 * hand onto the battlefield.
 *
 * The exert is [ExertAsItAttacks]; the "when you do" rider is its linked self-exert trigger.
 * `putFromHand`'s `ChooseUpTo(1)` carries the second "you may". The creature enters untapped and
 * not attacking.
 */
val ChampionOfRhonas = card("Champion of Rhonas") {
    manaCost = "{3}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Jackal Warrior"
    oracleText = "You may exert this creature as it attacks. When you do, you may put a creature card " +
        "from your hand onto the battlefield. (An exerted creature won't untap during your next untap step.)"
    power = 3
    toughness = 3

    staticAbility {
        ability = ExertAsItAttacks()
    }

    triggeredAbility {
        trigger = Triggers.self.exertedAsItAttacks()
        effect = Patterns.Hand.putFromHand(
            filter = GameObjectFilter.Creature,
            count = 1,
            prompt = "Put a creature card from your hand onto the battlefield?",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "159"
        artist = "Winona Nelson"
        flavorText = "The only way to finish the Trial of Strength is with a basilisk scale in hand."
        imageUri = "https://cards.scryfall.io/normal/front/b/1/b185b24d-ae33-48ca-966a-511627fdac6d.jpg?1783936478"
        ruling("2017-04-18", "All attackers are chosen at once. You can’t attack with Champion of Rhonas, put a creature card onto the battlefield, and then attack with that creature.")
        ruling("2017-04-18", "If the creature put onto the battlefield has any abilities that trigger when creatures attack or when you exert creatures, those abilities won’t trigger.")
        ruling("2017-04-18", "You can’t exert a creature unless an effect allows you to do so. Similar effects that “tap and freeze” a creature (such as that of Decision Paralysis) don’t exert that creature.")
        ruling("2017-04-18", "If an exerted creature is already untapped during your next untap step (most likely because it had vigilance or an effect untapped it), exert’s effect preventing it from untapping expires without having done anything.")
        ruling("2017-04-18", "If you gain control of another player’s creature until end of turn and exert it, it will untap during that player’s untap step.")
        ruling("2017-04-18", "All cards in the Amonkhet set that let you exert a creature let you do so as you declare it as an attacking creature, as do some of the cards in the Hour of Devastation set. You can’t do so later in combat, and creatures put onto the battlefield attacking can’t be exerted. Any abilities that trigger on exerting an attacking creature will resolve before blockers are declared.")
    }
}
