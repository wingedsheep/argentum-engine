package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity

/**
 * Vexing Bauble — Modern Horizons 3 #212.
 * {1} Artifact
 *
 * Whenever a player casts a spell, if no mana was spent to cast it, counter that spell.
 * {1}, {T}, Sacrifice this artifact: Draw a card.
 *
 * The counter clause is an intervening-if over the triggering spell's cast-mana record, so a spell
 * with mana value 0 cast normally (nothing paid) is countered too, not only "without paying its
 * mana cost" casts.
 */
val VexingBauble = card("Vexing Bauble") {
    manaCost = "{1}"
    typeLine = "Artifact"
    oracleText = "Whenever a player casts a spell, if no mana was spent to cast it, counter that spell.\n" +
        "{1}, {T}, Sacrifice this artifact: Draw a card."

    triggeredAbility {
        trigger = Triggers.anyPlayer.casts()
        interveningIf = Conditions.TriggeringSpellCastWithoutPayingMana
        effect = Effects.CounterTriggeringSpell()
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{1}"), Costs.Tap, Costs.SacrificeSelf)
        effect = Effects.DrawCards(1)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "212"
        flavorText = "No one knew who invented it. No one knew its purpose. All anyone knew was they didn't like being anywhere near it."
        artist = "Tony Foti"
        imageUri = "https://cards.scryfall.io/normal/front/2/9/29f11089-658f-42e6-aeb0-09b512ad2479.jpg?1783911242"
    }
}
