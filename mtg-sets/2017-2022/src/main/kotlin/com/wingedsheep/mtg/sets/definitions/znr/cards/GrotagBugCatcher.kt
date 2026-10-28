package com.wingedsheep.mtg.sets.definitions.znr.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Grotag Bug-Catcher — Zendikar Rising #142
 * {1}{R} · Creature — Goblin Warrior · 1/2
 *
 * Trample
 * Whenever this creature attacks, it gets +1/+0 until end of turn for each creature in your party.
 *
 * The party is counted as the trigger resolves ([DynamicAmounts.partySize]); the Bug-Catcher is a
 * Warrior itself, so it fills that slot when no other Warrior does.
 */
val GrotagBugCatcher = card("Grotag Bug-Catcher") {
    manaCost = "{1}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Goblin Warrior"
    power = 1
    toughness = 2
    oracleText = "Trample\n" +
        "Whenever this creature attacks, it gets +1/+0 until end of turn for each creature in your party. " +
        "(Your party consists of up to one each of Cleric, Rogue, Warrior, and Wizard.)"

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ModifyStats(DynamicAmounts.partySize(), DynamicAmounts.fixed(0), EffectTarget.Self)
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "142"
        artist = "Jakub Kasper"
        flavorText = "\"Everybody grab a leg!\""
        imageUri = "https://cards.scryfall.io/normal/front/5/b/5b310b01-f9cb-48ce-8af3-5957d3abf098.jpg?1783929359"
    }
}
