package com.wingedsheep.mtg.sets.definitions.aer.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Hidden Herbalists
 * {1}{G}
 * Creature — Human Druid
 * 2/2
 *
 * Revolt — When this creature enters, if a permanent left the battlefield under your control this
 * turn, add {G}{G}.
 *
 * Revolt is an intervening-if on [Conditions.YouHadPermanentLeaveBattlefieldThisTurn]; the mana is
 * Priest of Gix's enters-trigger [Effects.AddMana].
 */
val HiddenHerbalists = card("Hidden Herbalists") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Human Druid"
    oracleText = "Revolt — When this creature enters, if a permanent left the battlefield under your control " +
        "this turn, add {G}{G}."
    power = 2
    toughness = 2

    triggeredAbility {
        trigger = Triggers.self.enters()
        interveningIf = Conditions.YouHadPermanentLeaveBattlefieldThisTurn
        effect = Effects.AddMana(Color.GREEN, 2)
        description = "Revolt — When this creature enters, if a permanent left the battlefield under your " +
            "control this turn, add {G}{G}."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "110"
        artist = "Bastien L. Deharme"
        flavorText = "In the cellars beneath Narnam House, a strange and deadly garden grows."
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9ec8db01-415e-42a1-8eb2-57280d58b38e.jpg?1783936743"
        ruling("2017-02-09", "Revolt abilities check only whether a permanent you controlled left the battlefield this turn or not. They don't apply multiple times if more than one permanent you controlled left the battlefield. They don't check whether the permanent that left the battlefield is still in the zone it moved to.")
        ruling("2017-02-09", "Revolt abilities don't care why the permanent left the battlefield, who caused it to move, or where it moved to. They're equally satisfied by an artifact you sacrificed to pay a cost, a creature you controlled that was destroyed by Murder, or an enchantment you returned to your hand with Leave in the Dust.")
        ruling("2017-02-09", "Tokens that leave the battlefield will satisfy a revolt ability.")
        ruling("2017-02-09", "Energy counters aren't permanents. Paying {E} won't satisfy a revolt ability.")
        ruling("2017-02-09", "All cards in the Aether Revolt set with triggered revolt abilities use an intervening \"if\" clause. A permanent you controlled must have left the battlefield earlier in the turn in order for these abilities to trigger; otherwise they do nothing. In other words, there's no way to have the ability trigger if no permanent you controlled has left the battlefield that turn, even if you intend to have one do so in response to the triggered ability.")
    }
}
