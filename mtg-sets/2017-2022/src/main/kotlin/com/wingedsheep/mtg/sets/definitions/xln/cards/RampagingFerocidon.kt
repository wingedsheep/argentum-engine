package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.PreventLifeGain
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rampaging Ferocidon {2}{R} — Creature — Dinosaur 3/3
 *
 * Menace
 * Players can't gain life.
 * Whenever another creature enters, this creature deals 1 damage to that creature's controller.
 *
 * "Players can't gain life" is the same life-gain replacement Sunspine Lynx uses.
 */
val RampagingFerocidon = card("Rampaging Ferocidon") {
    manaCost = "{2}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Dinosaur"
    power = 3
    toughness = 3
    oracleText = "Menace\n" +
        "Players can't gain life.\n" +
        "Whenever another creature enters, this creature deals 1 damage to that creature's controller."

    keywords(Keyword.MENACE)

    replacementEffect(PreventLifeGain(appliesTo = EventPattern.LifeGainEvent(player = Player.Each)))

    triggeredAbility {
        trigger = Triggers.another(GameObjectFilter.Creature).enters()
        effect = Effects.DealDamage(1, EffectTarget.ControllerOfTriggeringEntity)
        description = "Whenever another creature enters, this creature deals 1 damage to that creature's controller."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "154"
        artist = "Jonathan Kuo"
        flavorText = "All raptors are aggressive, but ferocidons seem to enjoy their prey's pain."
        imageUri = "https://cards.scryfall.io/normal/front/3/9/39d3c658-1927-4af3-9077-88c4a669c730.jpg?1783935741"
        ruling("2017-09-29", "Spells and abilities that cause players to gain life still resolve while Rampaging Ferocidon is on the battlefield. No player will gain life, but any other effects of that spell or ability will happen.")
        ruling("2017-09-29", "If an effect says to set a player's life total to a number that's higher than the player's current life total while Rampaging Ferocidon is on the battlefield, the player's life total doesn't change.")
        ruling("2017-09-29", "Rampaging Ferocidon's last ability triggers whenever any player has a creature enter the battlefield, including you.")
        ruling("2017-09-29", "If another creature enters the battlefield at the same time as Rampaging Ferocidon, its last ability triggers.")
    }
}
