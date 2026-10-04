package com.wingedsheep.mtg.sets.definitions.mh3.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource

/**
 * Powerbalance
 * {R}{R}
 * Enchantment
 *
 * Whenever an opponent casts a spell, you may reveal the top card of your library. If you do,
 * you may cast that card without paying its mana cost if the two spells have the same mana value.
 *
 * The optional reveal is an [Effects.May] around the whole pipeline. The revealed top card is
 * filtered to a nonland card whose mana value equals the triggering spell's (captured at cast
 * time, so X is included per the ruling), then offered as a choose-up-to-one cast — the second
 * "may". The card stays on top of the library whether or not it's cast; the free cast happens
 * during resolution (`CastFromCollectionWithoutPayingCost` grants a library-scoped permission).
 *
 * Approximation: the comparison uses the revealed card's own mana value, so the prototype corner
 * case from the rulings (casting a card whose *resulting spell* matches) isn't modelled.
 */
val Powerbalance = card("Powerbalance") {
    manaCost = "{R}{R}"
    colorIdentity = "R"
    typeLine = "Enchantment"
    oracleText = "Whenever an opponent casts a spell, you may reveal the top card of your library. If you do, " +
        "you may cast that card without paying its mana cost if the two spells have the same mana value."

    triggeredAbility {
        trigger = Triggers.anOpponent.casts()
        effect = Effects.May(
            Effects.Pipeline {
                val revealed = gather(CardSource.TopOfLibrary(1), revealed = true)
                val matching = filter(
                    revealed,
                    GameObjectFilter.Nonland.manaValueEqualsDynamic(DynamicAmounts.triggeringSpellManaValue())
                )
                val chosen = chooseUpTo(
                    1,
                    from = matching,
                    selectedLabel = "Cast without paying its mana cost"
                )
                run(Effects.CastFromCollectionWithoutPayingCost(chosen))
            },
            prompt = "Reveal the top card of your library?"
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "131"
        artist = "Steve Ellis"
        flavorText = "For every action, a reaction. For every force, a firestorm."
        imageUri = "https://cards.scryfall.io/normal/front/a/f/aff7c0bb-1210-4aeb-b5f8-1387eb633b0f.jpg?1783911268"
        ruling("2024-06-07", "If an opponent casts a spell with {X} in its mana cost, use the value of X that was chosen when it was cast to determine its mana value.")
        ruling("2024-06-07", "You choose whether or not to cast the exiled card as Powerbalance's triggered ability resolves. If you do, you do so as part of the resolution of that ability. You can't wait to cast it later in the turn. Timing restrictions based on the card's type are ignored.")
        ruling("2024-06-07", "If the revealed card has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
        ruling("2024-06-07", "If you cast a spell \"without paying its mana cost,\" you can't choose to cast it for any alternative costs. You can, however, pay additional costs, such as kicker costs. If the spell has any mandatory additional costs, those must be paid to cast it.")
    }
}
