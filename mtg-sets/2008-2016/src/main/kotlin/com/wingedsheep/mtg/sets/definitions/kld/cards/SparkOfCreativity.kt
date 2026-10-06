package com.wingedsheep.mtg.sets.definitions.kld.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Spark of Creativity — Kaladesh #131
 * {R} · Sorcery
 *
 * Choose target creature. Exile the top card of your library. You may have Spark of Creativity
 * deal damage to that creature equal to the exiled card's mana value. If you don't, you may play
 * that card until end of turn.
 *
 * The "may" is an [Effects.May] whose `otherwise` is the may-play grant, so declining the damage
 * (and only declining it) leaves the exiled card playable until end of turn. The damage reads the
 * exiled collection's mana value (X counts as 0); an empty library exiles nothing and the damage
 * is 0. The creature is the spell's only target, so if it becomes illegal the spell does nothing.
 */
val SparkOfCreativity = card("Spark of Creativity") {
    manaCost = "{R}"
    colorIdentity = "R"
    typeLine = "Sorcery"
    oracleText = "Choose target creature. Exile the top card of your library. You may have Spark of " +
        "Creativity deal damage to that creature equal to the exiled card's mana value. If you " +
        "don't, you may play that card until end of turn."

    spell {
        val creature = target(TargetFilter.Creature)
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.TopOfLibrary(DynamicAmounts.fixed(1)))
            exile(exiled)
            run(Effects.May(
                Effects.DealDamage(DynamicAmounts.manaValueSumOf(exiled), creature),
                otherwise = Effects.GrantMayPlayFromExile(from = exiled),
                prompt = "Have Spark of Creativity deal damage to that creature equal to the exiled " +
                    "card's mana value? (If you don't, you may play that card until end of turn.)",
            ))
        }
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "131"
        artist = "Johann Bodin"
        flavorText = "\"To invent is to rebel.\"\n—Pia Nalaar"
        imageUri = "https://cards.scryfall.io/normal/front/7/1/718bf224-5e1b-439c-a998-ceec5c0a8903.jpg?1783937188"
        ruling("2016-09-20", "You can't cast Spark of Creativity without targeting a creature. If the creature becomes an illegal target for Spark of Creativity, none of its effects happen. You don't exile the top card of your library.")
        ruling("2016-09-20", "If a card in exile has {X} in its mana cost, X is considered to be 0.")
        ruling("2016-09-20", "If a land card is exiled this way, you can play that land card until end of turn if you have any available land plays.")
        ruling("2016-09-20", "Spark of Creativity doesn't change when you can play the exiled card. For example, if you exile a creature card without flash, you can cast it only during your main phase when the stack is empty.")
    }
}
