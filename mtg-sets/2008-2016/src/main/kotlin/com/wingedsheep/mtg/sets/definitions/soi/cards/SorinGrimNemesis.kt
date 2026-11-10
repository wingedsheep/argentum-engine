package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Sorin, Grim Nemesis (Shadows over Innistrad #251)
 * {4}{W}{B}
 * Legendary Planeswalker — Sorin
 * Loyalty 6
 *
 * +1: Reveal the top card of your library and put that card into your hand. Each opponent loses
 * life equal to its mana value.
 * −X: Sorin deals X damage to target creature or planeswalker and you gain X life.
 * −9: Create a number of 1/1 black Vampire Knight creature tokens with lifelink equal to the
 * highest life total among all players.
 *
 * The +1 is Dark Confidant's reveal-to-hand pipeline with the life loss aimed at each opponent;
 * an empty library reveals nothing and so costs nobody life. The −X reads the chosen X twice —
 * once for the damage, once for the life gain — so the gain doesn't depend on the damage landing
 * (a prevented hit still gains X), while an illegal target at resolution fizzles the whole
 * ability. The −9 counts with [DynamicAmounts.greatestAmongPlayers] over every player's life
 * total, read on resolution.
 */
val SorinGrimNemesis = card("Sorin, Grim Nemesis") {
    manaCost = "{4}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Planeswalker — Sorin"
    startingLoyalty = 6
    oracleText = "+1: Reveal the top card of your library and put that card into your hand. Each opponent loses life equal to its mana value.\n" +
        "−X: Sorin deals X damage to target creature or planeswalker and you gain X life.\n" +
        "−9: Create a number of 1/1 black Vampire Knight creature tokens with lifelink equal to the highest life total among all players."

    loyaltyAbility(+1) {
        effect = Effects.Pipeline {
            val revealed = gather(CardSource.TopOfLibrary(1, Player.You))
            toHand(revealed, revealed = true)
            run(Effects.LoseLife(
                DynamicAmounts.manaValueOf(revealed),
                EffectTarget.PlayerRef(Player.EachOpponent)
            ))
        }
        description = "Reveal the top card of your library and put it into your hand. Each opponent loses life equal to its mana value."
    }

    loyaltyAbilityX {
        val victim = target(Targets.CreatureOrPlaneswalker)
        effect = Effects.DealDamage(DynamicAmounts.xValue(), victim) then
            Effects.GainLife(DynamicAmounts.xValue())
        description = "Sorin deals X damage to target creature or planeswalker and you gain X life."
    }

    loyaltyAbility(-9) {
        effect = Effects.CreateToken(
            count = DynamicAmounts.greatestAmongPlayers(DynamicAmounts.lifeTotal(Player.You), Player.Each),
            power = 1,
            toughness = 1,
            colors = setOf(Color.BLACK),
            creatureTypes = setOf("Vampire", "Knight"),
            keywords = setOf(Keyword.LIFELINK)
        )
        description = "Create a 1/1 black Vampire Knight token with lifelink for each point of the highest life total among all players."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "251"
        artist = "Eric Deschamps"
        imageUri = "https://cards.scryfall.io/normal/front/c/2/c2933144-c931-498a-9e91-f5165873c3b7.jpg?1783937711"
        ruling("2016-04-08", "If a card in your library has {X} in its mana cost, X is considered to be 0.")
    }
}
