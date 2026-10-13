package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.readAhead
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Elder Dragon War
 * {2}{R}{R}
 * Enchantment — Saga
 *
 * Read ahead
 * I — This Saga deals 2 damage to each creature and each opponent.
 * II — Discard any number of cards, then draw that many cards.
 * III — Create a 4/4 red Dragon creature token with flying.
 *
 * Chapter I is Avengers: Under Siege's sweep-plus-opponents shape; chapter II is Into the Night's
 * `discardAnyNumber` → draw-that-many loot (discarding zero draws zero).
 */
val TheElderDragonWar = card("The Elder Dragon War") {
    manaCost = "{2}{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment — Saga"
    oracleText = "Read ahead (Choose a chapter and start with that many lore counters. Add one " +
        "after your draw step. Skipped chapters don't trigger. Sacrifice after III.)\n" +
        "I — This Saga deals 2 damage to each creature and each opponent.\n" +
        "II — Discard any number of cards, then draw that many cards.\n" +
        "III — Create a 4/4 red Dragon creature token with flying."

    readAhead()

    sagaChapter(1) {
        effect = Patterns.Group.dealDamageToAll(2, GroupFilter(GameObjectFilter.Creature)) then
            Effects.DealDamage(2, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    sagaChapter(2) {
        effect = Effects.Pipeline {
            val discarded = runStoringCollection { Patterns.Hand.discardAnyNumber(storeAs = it) }
            run(Effects.DrawCards(discarded.count))
        }
    }

    sagaChapter(3) {
        effect = Effects.CreateToken(
            power = 4,
            toughness = 4,
            colors = setOf(Color.RED),
            creatureTypes = setOf("Dragon"),
            keywords = setOf(Keyword.FLYING),
            imageUri = "https://cards.scryfall.io/normal/front/d/1/d1cae334-1e24-48a3-83a2-0a27bc9d3b29.jpg?1783921128",
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "121"
        artist = "Filip Burburan"
        imageUri = "https://cards.scryfall.io/normal/front/0/c/0c52c7d4-a350-4518-8c9a-72cf7b14603d.jpg?1783921321"

        ruling("2022-09-09", "In a Two-Headed Giant game, the first chapter ability deals to damage to each player on the opposing team, reducing that team's life total by 4.")
        ruling("2022-09-09", "As a Saga with read ahead enters the battlefield, its controller chooses a number from one to that Saga's greatest chapter number. The Saga enters the battlefield with the chosen number of lore counters. Neither choosing the number nor putting the counters on the Saga use the stack, and neither can be responded to.")
    }
}
