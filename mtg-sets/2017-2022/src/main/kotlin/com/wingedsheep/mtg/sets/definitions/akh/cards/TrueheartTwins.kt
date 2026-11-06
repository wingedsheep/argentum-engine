package com.wingedsheep.mtg.sets.definitions.akh.cards

import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ExertAsItAttacks
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Trueheart Twins — Amonkhet #153 (canonical printing)
 * {4}{R} · Creature — Jackal Warrior · 4/4
 *
 * You may exert this creature as it attacks.
 * Whenever you exert a creature, creatures you control get +1/+0 until end of turn.
 *
 * The pump trigger watches any exert by you — this creature's own attack exert included, or any
 * other creature you exert — and pumps the creatures you control as the trigger resolves.
 */
val TrueheartTwins = card("Trueheart Twins") {
    manaCost = "{4}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Jackal Warrior"
    oracleText = "You may exert this creature as it attacks. (It won't untap during your next untap step.)\n" +
        "Whenever you exert a creature, creatures you control get +1/+0 until end of turn."
    power = 4
    toughness = 4

    staticAbility {
        ability = ExertAsItAttacks()
    }

    triggeredAbility {
        trigger = Triggers.you.exerts(GameObjectFilter.Creature)
        effect = Patterns.Group.modifyStatsForAll(1, 0, GroupFilter(GameObjectFilter.Creature.youControl()))
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "153"
        artist = "Matt Stewart"
        flavorText = "Shoulder to shoulder until blade must turn against blade."
        imageUri = "https://cards.scryfall.io/normal/front/a/c/ac510911-1a65-4509-b6c9-ddac1950c838.jpg?1783936481"
        ruling("2017-04-18", "You can't exert a creature unless an effect allows you to do so. Similar effects that \"tap and freeze\" a creature (such as that of Decision Paralysis) don't exert that creature.")
        ruling("2017-04-18", "If an exerted creature is already untapped during your next untap step (most likely because it had vigilance or an effect untapped it), exert's effect preventing it from untapping expires without having done anything.")
        ruling("2017-04-18", "If you gain control of another player's creature until end of turn and exert it, it will untap during that player's untap step.")
        ruling("2017-04-18", "Some cards have abilities that trigger whenever you exert any creature. These abilities trigger when you exert that creature or any other creature you control.")
        ruling("2017-04-18", "All cards in the Amonkhet set that let you exert a creature let you do so as you declare it as an attacking creature, as do some of the cards in the Hour of Devastation set. You can't do so later in combat, and creatures put onto the battlefield attacking can't be exerted. Any abilities that trigger on exerting an attacking creature will resolve before blockers are declared.")
    }
}
