package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Silkweaver Elite
 * {2}{G}
 * Creature — Elf Archer
 * 2/2
 *
 * Reach
 * Revolt — When this creature enters, if a permanent left the battlefield under your control this
 * turn, draw a card.
 *
 * Revolt is an intervening-if on [Conditions.YouHadPermanentLeaveBattlefieldThisTurn], as on Hidden
 * Herbalists; the payoff is [Effects.DrawCards].
 */
val SilkweaverElite = card("Silkweaver Elite") {
    manaCost = "{2}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Elf Archer"
    oracleText = "Reach (This creature can block creatures with flying.)\n" +
        "Revolt — When this creature enters, if a permanent left the battlefield under your control " +
        "this turn, draw a card."
    power = 2
    toughness = 2

    keywords(Keyword.REACH)

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouHadPermanentLeaveBattlefieldThisTurn
        effect = Effects.DrawCards(1)
        description = "Revolt — When this creature enters, if a permanent left the battlefield under your " +
            "control this turn, draw a card."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "125"
        artist = "Magali Villeneuve"
        flavorText = "Elvish weavers use spidersilk to make everything from tapestries to rappelling ropes."
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d10938c8-6fc4-495f-912a-1d3f12278f78.jpg?1783936737"
        ruling("2017-02-09", "Revolt abilities check only whether a permanent you controlled left the battlefield this turn or not. They don't apply multiple times if more than one permanent you controlled left the battlefield. They don't check whether the permanent that left the battlefield is still in the zone it moved to.")
        ruling("2017-02-09", "Revolt abilities don't care why the permanent left the battlefield, who caused it to move, or where it moved to. They're equally satisfied by an artifact you sacrificed to pay a cost, a creature you controlled that was destroyed by Murder, or an enchantment you returned to your hand with Leave in the Dust.")
        ruling("2017-02-09", "Tokens that leave the battlefield will satisfy a revolt ability.")
        ruling("2017-02-09", "Energy counters aren't permanents. Paying {E} won't satisfy a revolt ability.")
        ruling("2017-02-09", "All cards in the Aether Revolt set with triggered revolt abilities use an intervening \"if\" clause. A permanent you controlled must have left the battlefield earlier in the turn in order for these abilities to trigger; otherwise they do nothing. In other words, there's no way to have the ability trigger if no permanent you controlled has left the battlefield that turn, even if you intend to have one do so in response to the triggered ability.")
    }
}
