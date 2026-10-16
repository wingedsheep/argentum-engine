package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.CounterType
import com.wingedsheep.sdk.dsl.*
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CantBeBlocked
import com.wingedsheep.sdk.scripting.ConditionalStaticAbility
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.WardCost
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Nightkin Ambusher — the evasion gate reads the rad counters of *this creature's* defending
 * player (`Player.DefendingPlayer` resolves through the Ambusher's own attack assignment), so in
 * multiplayer it is the player it attacks, not any opponent. Blockability is checked as blockers
 * are declared; a rad counter gained after it is blocked doesn't unblock it (2024-03-08 ruling).
 */
val NightkinAmbusher = card("Nightkin Ambusher") {
    manaCost = "{2}{U}{B}"
    typeLine = "Creature — Mutant Warrior"
    power = 4
    toughness = 4
    oracleText = "Ward {2} (Whenever this creature becomes the target of a spell or ability an opponent controls, counter it unless that player pays {2}.)\n" +
        "When this creature enters, target player gets four rad counters.\n" +
        "This creature can't be blocked as long as defending player has a rad counter."

    keywordAbility(KeywordAbility.Ward(WardCost.Mana("{2}")))

    triggeredAbility {
        trigger = Triggers.self.enters()
        val player = target(Targets.Player)
        effect = Effects.AddCounters(CounterType.RAD, 4, player)
    }

    staticAbility {
        ability = ConditionalStaticAbility(
            ability = CantBeBlocked(),
            condition = Conditions.CompareAmounts(
                DynamicAmounts.playerCounterCount(CounterType.RAD, Player.DefendingPlayer),
                ComparisonOperator.GTE,
                1
            )
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "112"
        artist = "Antonio José Manzanedo"
        imageUri = "https://cards.scryfall.io/normal/front/c/a/ca7cfcb2-83fb-41ad-ae31-9c32c942506c.jpg?1783912385"
        ruling(
            "2024-03-08",
            "Once Nightkin Ambusher has been blocked, giving the defending player a rad counter won't cause Nightkin Ambusher to become unblocked."
        )
    }
}
