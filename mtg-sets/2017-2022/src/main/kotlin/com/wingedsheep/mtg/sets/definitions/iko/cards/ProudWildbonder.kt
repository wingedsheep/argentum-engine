package com.wingedsheep.mtg.sets.definitions.iko.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AssignCombatDamageAsUnblocked
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Proud Wildbonder — Ikoria: Lair of Behemoths #229
 * {2}{R/G}{R/G} · Creature — Human Warrior · 4/3
 *
 * The granted ability is a battlefield-scoped [AssignCombatDamageAsUnblocked] over creatures you
 * control with trample; the keyword is read off projected state at combat damage, so a creature
 * that gained or lost trample is covered accordingly — including Proud Wildbonder itself.
 */
val ProudWildbonder = card("Proud Wildbonder") {
    manaCost = "{2}{R/G}{R/G}"
    typeLine = "Creature — Human Warrior"
    power = 4
    toughness = 3
    oracleText = "Trample\n" +
        "Creatures you control with trample have \"You may have this creature assign its combat damage " +
        "as though it weren't blocked.\""

    keywords(Keyword.TRAMPLE)

    staticAbility {
        ability = AssignCombatDamageAsUnblocked(
            filter = GroupFilter(GameObjectFilter.Creature.youControl().withKeyword(Keyword.TRAMPLE))
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "229"
        artist = "Dmitry Burmak"
        flavorText = "\"You're little now, yes, but one day your footsteps will make mountains tremble.\"\n—Gustin, wildbonder"
        imageUri = "https://cards.scryfall.io/normal/front/f/4/f4876e17-a206-4351-9c0b-0845db4569a3.jpg?1783931008"
        ruling("2020-04-17", "Proud Wildbonder's last ability applies to itself as long as it still has trample.")
        ruling("2020-04-17", "When assigning combat damage, you choose whether you want to assign all damage to blocking creatures and assign trample damage from that as normal, or if you want to assign all of it to the player or planeswalker the creature is attacking. You can't assign some damage to one blocker then skip other blockers.")
    }
}
