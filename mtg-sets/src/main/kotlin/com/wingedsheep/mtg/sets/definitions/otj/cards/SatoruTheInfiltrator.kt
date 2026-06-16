package com.wingedsheep.mtg.sets.definitions.otj.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect

/**
 * Satoru, the Infiltrator
 * {U}{B}
 * Legendary Creature — Human Ninja Rogue
 * 2/3
 *
 * Menace
 * Whenever Satoru and/or one or more other nontoken creatures you control enter, if none of them
 * were cast or no mana was spent to cast them, draw a card.
 *
 * The trigger is a batch enters over nontoken creatures you control ([Triggers.OneOrMorePermanentsEnter];
 * Satoru itself qualifies). The intervening "if none of them were cast or no mana was spent" is the
 * batch-level [Conditions.NoManaSpentToCastEntered], evaluated at resolution over the captured batch:
 * if every entered creature had no mana spent to cast it, draw a card. When Satoru is hard-cast for
 * mana it's in the batch but had mana spent, so the condition fails and you don't draw; a blinked /
 * reanimated / token-free-cast creature entering while Satoru is in play passes and draws.
 */
val SatoruTheInfiltrator = card("Satoru, the Infiltrator") {
    manaCost = "{U}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Human Ninja Rogue"
    power = 2
    toughness = 3
    oracleText = "Menace\n" +
        "Whenever Satoru and/or one or more other nontoken creatures you control enter, if none " +
        "of them were cast or no mana was spent to cast them, draw a card."

    keywords(Keyword.MENACE)

    triggeredAbility {
        trigger = Triggers.OneOrMorePermanentsEnter(GameObjectFilter.Creature.nontoken())
        effect = ConditionalEffect(
            condition = Conditions.NoManaSpentToCastEntered,
            effect = Effects.DrawCards(1)
        )
        description = "Whenever Satoru and/or one or more other nontoken creatures you control " +
            "enter, if none of them were cast or no mana was spent to cast them, draw a card."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "230"
        artist = "Heonhwa"
        flavorText = "\"This backwater doesn't have a single wall, ward, or guard that could keep me out.\""
        imageUri = "https://cards.scryfall.io/normal/front/a/c/acc9a5cc-2b3c-4c2f-8176-4a2d86265cc5.jpg?1712356202"

        ruling("2024-04-12", "If you cast a creature spell without paying its mana cost but you paid mana for additional costs or cost increases (such as from Aven Interrupter), Satoru's last ability won't trigger.")
    }
}
