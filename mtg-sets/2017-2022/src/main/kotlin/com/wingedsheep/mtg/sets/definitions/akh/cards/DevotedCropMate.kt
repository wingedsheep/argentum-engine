package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ExertAsItAttacks
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Devoted Crop-Mate — Amonkhet #10 (canonical printing)
 * {2}{W} · Creature — Human Warrior · 3/2
 *
 * You may exert this creature as it attacks. When you do, return target creature card with mana
 * value 2 or less from your graveyard to the battlefield.
 *
 * The exert is [ExertAsItAttacks]; the "when you do" rider is its linked self-exert trigger. The
 * returned creature arrives after attackers are declared, so it isn't attacking (2017-04-18 ruling).
 */
val DevotedCropMate = card("Devoted Crop-Mate") {
    manaCost = "{2}{W}"
    colorIdentity = "W"
    typeLine = "Creature — Human Warrior"
    oracleText = "You may exert this creature as it attacks. When you do, return target creature card with " +
        "mana value 2 or less from your graveyard to the battlefield. " +
        "(An exerted creature won't untap during your next untap step.)"
    power = 3
    toughness = 2

    staticAbility {
        ability = ExertAsItAttacks()
    }

    triggeredAbility {
        trigger = Triggers.self.exertedAsItAttacks()
        val creature = target(
            TargetFilter(GameObjectFilter.Creature.ownedByYou().manaValueAtMost(2), zone = Zone.GRAVEYARD)
        )
        effect = Effects.Move(creature, Zone.BATTLEFIELD, fromZone = Zone.GRAVEYARD)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "10"
        artist = "Zoltan Boros"
        imageUri = "https://cards.scryfall.io/normal/front/d/3/d3677f10-b530-4606-b7c8-a72dd2131160.jpg?1783936540"
        ruling("2017-04-18", "The mana value of a card in your graveyard is determined solely by the mana symbols printed in its upper right corner. The mana value is the total amount of mana in that cost, regardless of color. For example, a card with mana cost {1}{U}{U} has mana value 3.")
        ruling("2017-04-18", "If the mana cost of a card in your graveyard includes X, X is considered to be 0.")
        ruling("2017-04-18", "All attackers are chosen at once. You can't attack with Devoted Crop-Mate, return a creature card to the battlefield, and then attack with that creature.")
        ruling("2017-04-18", "If the creature returned to the battlefield has any abilities that trigger when creatures attack or when you exert creatures, those abilities won't trigger.")
        ruling("2017-04-18", "If a creature has a targeted triggered ability that triggers when you exert it, you can exert it even if there isn't a legal target for that triggered ability.")
        ruling("2017-04-18", "All cards in the Amonkhet set that let you exert a creature let you do so as you declare it as an attacking creature, as do some of the cards in the Hour of Devastation set. You can't do so later in combat, and creatures put onto the battlefield attacking can't be exerted. Any abilities that trigger on exerting an attacking creature will resolve before blockers are declared.")
        ruling("2017-04-18", "If an exerted creature is already untapped during your next untap step (most likely because it had vigilance or an effect untapped it), exert's effect preventing it from untapping expires without having done anything.")
    }
}
